package com.appian.intellij.k.debugger;

import java.util.Map;

import com.google.common.collect.Maps;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;

public class ExpressionEvaluationService {
  protected Map<Long, XDebuggerEvaluator.XEvaluationCallback> pendingEvaluations = Maps.newHashMap();
  protected long transactionId = 0L;

  public ExpressionEvaluationService() {}

  /**
   * Retrieves and removes the callback from the pending evaluations map
   * @param id the transactionId of the evaluation
   * @return the callback associated with the given transactionId
   */
  public XDebuggerEvaluator.XEvaluationCallback getCallbackById(Long id) {
    return pendingEvaluations.remove(id);
  }

  /**
   * Registers an evaluation request under a new transactionId
   * @param evaluationCallback
   * @return the unique transactionId for this evaluation cycle
   */
  public long addPendingEvaluation(XDebuggerEvaluator.XEvaluationCallback evaluationCallback) {
    pendingEvaluations.put(transactionId, evaluationCallback);
    return transactionId++;
  }
}
