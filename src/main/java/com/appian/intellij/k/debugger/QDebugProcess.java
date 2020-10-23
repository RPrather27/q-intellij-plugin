package com.appian.intellij.k.debugger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XSuspendContext;

public class QDebugProcess extends XDebugProcess {
  private final QBreakpointReachedHandler qBreakpointReachedHandler;
  private RunProfileState state;
  private ExecutionResult execute;
  private QBreakpointHandler breakpointHandler;
  private QBreakpointReachedFileWatcher fileWatchThread;
  private final BreakpointService breakpointService;
  XDebugSession session;

  public QDebugProcess(XDebugSession session, RunProfileState state, ExecutionResult execute) {
    super(session);
    this.session = session;
    this.state = state;
    this.execute = execute;
    breakpointService = new BreakpointService();
    this.breakpointHandler = new QBreakpointHandler(QBreakpointType.class, breakpointService);
    qBreakpointReachedHandler = new QBreakpointReachedHandler(breakpointService, getSession());
    initializeWatch();
    terminateOnTestEnd();
  }

  @NotNull
  @Override
  public XBreakpointHandler<?>[] getBreakpointHandlers() {
    return new QBreakpointHandler[] {breakpointHandler};
  }

  @NotNull
  @Override
  public XDebuggerEditorsProvider getEditorsProvider() {
    return QDebuggerEditorsProvider.INSTANCE;
  }

  @Override
  public void stop() {
    fileWatchThread.interrupt();
    breakpointHandler.unregisterAllBreakpoints();
  }

  @Override
  public void resume(@Nullable XSuspendContext context) {
    if (fileWatchThread.atBreakpoint()) {
      fileWatchThread.deleteCurrentBrqnFile();
      getSession().resume();
    }
  }

  private void initializeWatch() {
    fileWatchThread = new QBreakpointReachedFileWatcher(qBreakpointReachedHandler);
    fileWatchThread.start();
  }

  private void terminateOnTestEnd() {
    final Disposable disposable = Disposer.newDisposable();
    Project project = session.getProject();
    Disposer.register(project, disposable);
    project.getMessageBus().connect(disposable).subscribe(SMTRunnerEventsListener.TEST_STATUS, new SMTRunnerEventsListener() {
      boolean isStopped = false;

      @Override
      public void onTestingStarted(@NotNull SMTestProxy.SMRootTestProxy testsRoot) {
      }

      @Override
      public void onTestingFinished(@NotNull SMTestProxy.SMRootTestProxy testsRoot) {
        if (!isStopped) {
          isStopped = true;
          session.stop();
        }
      }

      @Override
      public void onTestsCountInSuite(int count) {

      }

      @Override
      public void onTestStarted(@NotNull SMTestProxy test) {

      }

      @Override
      public void onTestFinished(@NotNull SMTestProxy test) {

      }

      @Override
      public void onTestFailed(@NotNull SMTestProxy test) {

      }

      @Override
      public void onTestIgnored(@NotNull SMTestProxy test) {

      }

      @Override
      public void onSuiteFinished(@NotNull SMTestProxy suite) {
        if (!isStopped) {
          isStopped = true;
          session.stop();
        }
      }

      @Override
      public void onSuiteStarted(@NotNull SMTestProxy suite) {

      }

      @Override
      public void onCustomProgressTestsCategory(@Nullable String categoryName, int testCount) {

      }

      @Override
      public void onCustomProgressTestStarted() {

      }

      @Override
      public void onCustomProgressTestFailed() {

      }

      @Override
      public void onCustomProgressTestFinished() {

      }

      @Override
      public void onSuiteTreeNodeAdded(SMTestProxy testProxy) {

      }

      @Override
      public void onSuiteTreeStarted(SMTestProxy suite) {

      }
    });

  }
}
