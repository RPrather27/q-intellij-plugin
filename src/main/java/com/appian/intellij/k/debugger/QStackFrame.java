package com.appian.intellij.k.debugger;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XStackFrame;

public class QStackFrame extends XStackFrame {
  private final int lineNumber;
  private final VirtualFile virtualFile;

  public QStackFrame(VirtualFile virtualFile, int lineNumber) {
    this.virtualFile = virtualFile;
    this.lineNumber = lineNumber;
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
