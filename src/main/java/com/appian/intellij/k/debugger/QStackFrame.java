package com.appian.intellij.k.debugger;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Scanner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XNamedValue;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;

public class QStackFrame extends XStackFrame {
  private final int lineNumber;
  private final VirtualFile virtualFile;
  private final JsonObject jsonData;

  public QStackFrame(VirtualFile virtualFile, int lineNumber) {
    this.virtualFile = virtualFile;
    this.lineNumber = lineNumber;
    File f = new File("/tmp/breakpoint/" + virtualFile.getName() + "_" + (lineNumber+1) + ".brqn");
    String data = "";
    try (Scanner myReader = new Scanner(f)) {
      while (myReader.hasNextLine()) {
        data += myReader.nextLine();
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    jsonData = (JsonObject)new JsonParser().parse(data);
  }

  @Override
  public void customizePresentation(@NotNull ColoredTextContainer component) {
    int lineNumberDisplay = lineNumber + 1; // Line number is 0-indexed, so the display should add 1
    String nameAndLine = virtualFile.getName() + ":" + lineNumberDisplay + ", ";
    component.append(nameAndLine, SimpleTextAttributes.REGULAR_ATTRIBUTES);

    String filePath = virtualFile.getPath();
    component.append(filePath, SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);

    component.setIcon(AllIcons.Debugger.Frame);
  }

  @Nullable
  @Override
  public XSourcePosition getSourcePosition() {
    if (virtualFile != null) {
      return XDebuggerUtil.getInstance().createPosition(virtualFile, lineNumber);
    }
    return null;
  }

  /**
   * Hooks into IntelliJ debugger API to display variables for a stackframe
   * Displays the SAIL bindings at the given stack frame
   */
  @Override
  public void computeChildren(@NotNull XCompositeNode node) {
    if (jsonData != null && !jsonData.isJsonNull()) {
      XValueChildrenList childrenList = getChildrenListFromBindings(jsonData);
      node.addChildren(childrenList, true);
    } else {
      super.computeChildren(node);
    }
  }

 public XValueChildrenList getChildrenListFromBindings(JsonObject bindings) {
    XValueChildrenList childrenList = new XValueChildrenList();
    for (String key : bindings.keySet()) {
      childrenList.add(new XNamedValue(key) {
        @Override
        public void computePresentation(
            @NotNull XValueNode node, @NotNull XValuePlace place) {
          JsonObject variableJson = (JsonObject)new JsonParser().parse(bindings.get(key).toString());
          node.setPresentation(null, variableJson.get("type").toString(), variableJson.get("data").toString(), false);
        }
      });
    }
    return childrenList;
  }

  /**
   * Do something like JavaDebuggerEvaluator instead :D
   *
   * Having this call block makes the IntelliJ process impossible to kill (intelliJ process is called 'Main' in activity monitor FYI)
   */
  @Nullable
  @Override
  public XDebuggerEvaluator getEvaluator() {
    return new XDebuggerEvaluator() {
      @Override
      public void evaluate(@NotNull String expression, @NotNull XEvaluationCallback callback,
                           @Nullable XSourcePosition expressionPosition) {
        try {
          File brkpointDir = new File("/tmp/breakpoint/debug.runny");
          OutputStream out = new FileOutputStream(brkpointDir);
          out.write("HI".getBytes());
          out.flush();
          out.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
        WatchService ws;
        try {
          ws = FileSystems.getDefault().newWatchService();
          while (true) {
            try {
              WatchKey watchKey = ws.poll();
              Thread.sleep(1000);
              try {
                for (WatchEvent<?> event : watchKey.pollEvents()) {
                  if (event.kind() != ENTRY_CREATE) {
                    continue;
                  }

                  WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                  Path filename = pathEvent.context();
                  if (filename == null) {
                    System.out.println("Skipping: null context");
                    continue;
                  }

                  if (!filename.toString().endsWith(".RETURNS")) {
                    continue;
                  }

                  String s = filename.toString();
                  return;
                }
              } finally {
                // want to make sure we _always_ reset the watch key
                watchKey.reset();
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };
  }
}
