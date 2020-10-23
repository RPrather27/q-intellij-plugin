package com.appian.intellij.k.debugger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
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
   * Hooks into IntelliJ debugger API to display variables for a stackframe
   * Displays the SAIL bindings at the given stack frame
   */
  @Override
  public void computeChildren(@NotNull XCompositeNode node) {
//    List<SailBinding> bindings = descriptor.getBindings();
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
}
