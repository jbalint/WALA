package com.ibm.wala.demandpa.alg;

import java.util.Iterator;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey.TypeFilter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
  /**
   * a {@link HeapModel} that delegates to another except for pointer keys
   * representing <code>this</code> parameters of methods, for which it
   * returns a {@link FilteredPointerKey} for the type of the parameter
   * 
   * @see #getPointerKeyForLocal(CGNode, int)
   * @author manu
   * 
   */
  public class ThisFilteringHeapModel implements HeapModel {

    private final HeapModel delegate;

    private final IClassHierarchy cha;

    public IClassHierarchy getClassHierarchy() {
      return delegate.getClassHierarchy();
    }

    public FilteredPointerKey getFilteredPointerKeyForLocal(CGNode node, int valueNumber, TypeFilter filter) {
      return delegate.getFilteredPointerKeyForLocal(node, valueNumber, filter);
    }

    public InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation) {
      return delegate.getInstanceKeyForAllocation(node, allocation);
    }

    public InstanceKey getInstanceKeyForClassObject(TypeReference type) {
      return delegate.getInstanceKeyForClassObject(type);
    }

    public InstanceKey getInstanceKeyForConstant(TypeReference type, Object S) {
      return delegate.getInstanceKeyForConstant(type, S);
    }

    public InstanceKey getInstanceKeyForMultiNewArray(CGNode node, NewSiteReference allocation, int dim) {
      return delegate.getInstanceKeyForMultiNewArray(node, allocation, dim);
    }

    public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter instr, TypeReference type) {
      return delegate.getInstanceKeyForPEI(node, instr, type);
    }

    public PointerKey getPointerKeyForArrayContents(InstanceKey I) {
      return delegate.getPointerKeyForArrayContents(I);
    }

    public PointerKey getPointerKeyForExceptionalReturnValue(CGNode node) {
      return delegate.getPointerKeyForExceptionalReturnValue(node);
    }

    public PointerKey getPointerKeyForInstanceField(InstanceKey I, IField field) {
      return delegate.getPointerKeyForInstanceField(I, field);
    }

    public PointerKey getPointerKeyForLocal(CGNode node, int valueNumber) {
      if (!node.getMethod().isStatic() && valueNumber == 1) {
        return delegate.getFilteredPointerKeyForLocal(node, valueNumber, getFilter(node));
      } else {
        return delegate.getPointerKeyForLocal(node, valueNumber);
      }
    }

    private FilteredPointerKey.TypeFilter getFilter(CGNode target) {
      FilteredPointerKey.TypeFilter filter = (FilteredPointerKey.TypeFilter) target.getContext().get(ContextKey.FILTER);

      if (filter != null) {
        return filter;
      } else {
        // the context does not select a particular concrete type for the
        // receiver.
        IClass C = getReceiverClass(target.getMethod());
        return new FilteredPointerKey.SingleClassFilter(C);
      }
    }

    /**
     * @param method
     * @return the receiver class for this method.
     */
    private IClass getReceiverClass(IMethod method) {
      TypeReference formalType = method.getParameterType(0);
      IClass C = cha.lookupClass(formalType);
      if (Assertions.verifyAssertions) {
        if (method.isStatic()) {
          Assertions.UNREACHABLE("asked for receiver of static method " + method);
        }
        if (C == null) {
          Assertions.UNREACHABLE("no class found for " + formalType + " recv of " + method);
        }
      }
      return C;
    }

    public PointerKey getPointerKeyForReturnValue(CGNode node) {
      return delegate.getPointerKeyForReturnValue(node);
    }

    public PointerKey getPointerKeyForStaticField(IField f) {
      return delegate.getPointerKeyForStaticField(f);
    }

    public Iterator<PointerKey> iteratePointerKeys() {
      return delegate.iteratePointerKeys();
    }

    public ThisFilteringHeapModel(HeapModel delegate, IClassHierarchy cha) {
      this.delegate = delegate;
      this.cha = cha;
    }

  }

