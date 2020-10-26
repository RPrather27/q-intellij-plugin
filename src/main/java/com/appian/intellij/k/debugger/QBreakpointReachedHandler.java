package com.appian.intellij.k.debugger;

import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.impl.breakpoints.XLineBreakpointImpl;

public class QBreakpointReachedHandler {

  private BreakpointService breakpointService;
  private XDebugSession debugSession;

  public QBreakpointReachedHandler(
      BreakpointService breakpointService, XDebugSession debugSession) {
    this.breakpointService = breakpointService;
    this.debugSession = debugSession;
  }

  public void process(String fileName, int lineNumber) {
    XLineBreakpoint breakpointReached = null;
    QSuspendContext qSuspendContext = null;
    for (XLineBreakpoint breakpoint : breakpointService.getXLineBreakpoints()) {
      XLineBreakpointImpl breakpointImpl = (XLineBreakpointImpl) breakpoint;
      if (fileName.equals(breakpointImpl.getFile().getName()) && breakpoint.getLine() == lineNumber) {
        qSuspendContext = new QSuspendContext(breakpointImpl.getFile(), lineNumber);
        breakpointReached = breakpoint;
      }
    }
    if (breakpointReached != null & qSuspendContext != null) {
      debugSession.breakpointReached(breakpointReached, null, qSuspendContext);
      debugSession.positionReached(qSuspendContext);
    }
  }
}
