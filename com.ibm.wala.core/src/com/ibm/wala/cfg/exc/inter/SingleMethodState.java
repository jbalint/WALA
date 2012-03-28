package com.ibm.wala.cfg.exc.inter;

import java.util.HashMap;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.exc.ExceptionPruningAnalysis;
import com.ibm.wala.cfg.exc.intra.NullPointerState;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

/**
 * Saves interprocedural state of a single method.
 * 
 * @author Markus Herhoffer <markus.herhoffer@student.kit.edu>
 * @author Juergen Graf <graf@kit.edu>
 * 
 */
final class SingleMethodState {

  private final ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg;
  private final HashMap<IExplodedBasicBlock, String> statesOfSsaVars = new HashMap<IExplodedBasicBlock, String>();
  private final HashMap<IExplodedBasicBlock, Object[]> valuesOfSsaVars = new HashMap<IExplodedBasicBlock, Object[]>();
  private final HashMap<IExplodedBasicBlock, int[]> numbersOfSsaVarsThatAreParemerters = new HashMap<IExplodedBasicBlock, int[]>();
  private final boolean hasNoRecords;
  private boolean throwsException = true;

  /**
   * Constructor for an emtpy OptimisationInfo.
   * 
   * Use it if you have nothing to tell about the node.
   */
  SingleMethodState() {
    this.cfg = null;
    this.hasNoRecords = true;
  }

  /**
   * Constructor if you have informations on the node.
   * 
   * All values are saved at construction time. So if the analysis changes
   * anything after this OptimizationInfo was created, it won't affect its final
   * attributes.
   * 
   * @param intra
   *          The <code>node</code>'s intraprocedural analysis
   * @param node
   *          the node itself
   * @throws UnsoundGraphException
   * @throws CancelException
   */
  SingleMethodState(final ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intra, final CGNode node,
      final ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg) throws UnsoundGraphException, CancelException {
    this.cfg = cfg;
    this.hasNoRecords = false;
    final SymbolTable sym = node.getIR().getSymbolTable();
    
    for (final IExplodedBasicBlock block : cfg) {
      // set states
      final NullPointerState state = intra.getState(block);
      this.statesOfSsaVars.put(block, state.toString());

      // set values
      if (block.getInstruction() != null) {
        final int numberOfSSAVars = block.getInstruction().getNumberOfUses();
        final Object[] values = new Object[numberOfSSAVars];

        for (int j = 0; j < numberOfSSAVars; j++) {
          final boolean isContant = sym.isConstant(j);
          values[j] = isContant ? sym.getConstantValue(j) : null;
        }

        this.valuesOfSsaVars.put(block, values);
      } else {
        this.valuesOfSsaVars.put(block, null);
      }

      // set nr. of parameters
      if (block.getInstruction() instanceof SSAAbstractInvokeInstruction) {
        final SSAAbstractInvokeInstruction instr = (SSAAbstractInvokeInstruction) block.getInstruction();
        final int[] numbersOfParams = InterprocNullPointerAnalysis.getParameterNumbers(instr);
        this.numbersOfSsaVarsThatAreParemerters.put(block, numbersOfParams);
      } else {
        // default to null
        this.numbersOfSsaVarsThatAreParemerters.put(block, null);
      }
    }
  }

  public String getState(final IExplodedBasicBlock block) {
    // if (hasNoRecords) throw new IllegalStateException();
    return statesOfSsaVars.get(block);
  }

  public Object[] getValues(final IExplodedBasicBlock block) {
    if (hasNoRecords) {
      throw new IllegalStateException();
    }

    return valuesOfSsaVars.get(block);
  }

  public int[] getInjectedParameters(final IExplodedBasicBlock block) {
    if (hasNoRecords) {
      throw new IllegalStateException();
    } else if (!((block.getInstruction() instanceof SSAAbstractInvokeInstruction))) {
      throw new IllegalStateException();
    }

    assert (block.getInstruction() instanceof SSAAbstractInvokeInstruction);

    return numbersOfSsaVarsThatAreParemerters.get(block);
  }

  /**
   * Returns the CFG.
   * 
   * @return the CFG or null if there is no CFG for the CGNode.
   */
  public ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> getCFG() {
    return (hasNoRecords ? null : this.cfg);
  }

  public boolean hasRecords() {
    return !hasNoRecords;
  }

  public void setThrowsException(final boolean throwsException) {
    this.throwsException = throwsException;
  }

  public boolean throwsException() {
    return throwsException;
  }

  @Override
  public String toString() {
    if (hasNoRecords) {
      return "";
    }

    final String ls = System.getProperty("line.separator");
    final StringBuffer output = new StringBuffer();
    output.append(statesOfSsaVars.toString() + ls);
    output.append(valuesOfSsaVars.toString() + ls);
    output.append(numbersOfSsaVarsThatAreParemerters.toString());

    return output.toString();
  }

}
