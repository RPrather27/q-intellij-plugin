package com.appian.intellij.k.debugger;

import java.io.File;
import java.io.IOException;

import org.jetbrains.annotations.NotNull;

import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.impl.breakpoints.XLineBreakpointImpl;

public class QBreakpointHandler extends XBreakpointHandler {
  private BreakpointService breakpointService;

  protected QBreakpointHandler(
      @NotNull Class breakpointTypeClass, BreakpointService breakpointService) {
    super(breakpointTypeClass);
    this.breakpointService = breakpointService;

    File brkpointDir = new File("/tmp/breakpoint");
    if (!brkpointDir.exists()) {
      brkpointDir.mkdir();
    } else {
      for (File oldBrkFile : brkpointDir.listFiles()) {
          oldBrkFile.delete();
      }
    }
  }

  @Override
  public void registerBreakpoint(@NotNull XBreakpoint breakpoint) {
    String fileName = ((XLineBreakpointImpl) breakpoint).getFile().getName();
    int line = ((XLineBreakpointImpl) breakpoint).getLine() + 1;

    File f = new File("/tmp/breakpoint/" + fileName + "_" + line + ".brq");
    try {
      f.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    breakpointService.addBreakpoint((XLineBreakpoint) breakpoint);
  }

  @Override
  public void unregisterBreakpoint(@NotNull XBreakpoint breakpoint, boolean temporary) {
    String fileName = ((XLineBreakpointImpl) breakpoint).getFile().getName();
    int line = ((XLineBreakpointImpl) breakpoint).getLine() + 1;
    File f = new File("/tmp/breakpoint/" + fileName + "_" + line + ".brq");
    f.delete();
    breakpointService.removeBreakpoint((XLineBreakpoint) breakpoint);
  }

  public void unregisterAllBreakpoints() {
    for (XLineBreakpoint breakpoint : breakpointService.getXLineBreakpoints()) {
      unregisterBreakpoint(breakpoint, false);
    }
  }
}
