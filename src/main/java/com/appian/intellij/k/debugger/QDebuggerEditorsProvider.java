package com.appian.intellij.k.debugger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.appian.intellij.k.KFileType;
import com.appian.intellij.k.psi.KElementFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProviderBase;

public class QDebuggerEditorsProvider extends XDebuggerEditorsProviderBase {
  public static final QDebuggerEditorsProvider INSTANCE = new QDebuggerEditorsProvider();

  private QDebuggerEditorsProvider() {
    super();
  }

  @Override
  protected PsiFile createExpressionCodeFragment(@NotNull Project project, @NotNull String text, @Nullable PsiElement context, boolean isPhysical) {
    return KElementFactory.createFile(project, text, true);
  }

  @NotNull
  @Override
  public FileType getFileType() {
    return KFileType.INSTANCE;
  }
}
