/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.6
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.janelia.simview.klb.jni;

@Deprecated
public class KlbRoi {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected KlbRoi(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(KlbRoi obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        KlbJNI.delete_KlbRoi(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setXyzctLB(long[] value) {
    KlbJNI.KlbRoi_xyzctLB_set(swigCPtr, this, value);
  }

  public long[] getXyzctLB() {
    return KlbJNI.KlbRoi_xyzctLB_get(swigCPtr, this);
  }

  public void setXyzctUB(long[] value) {
    KlbJNI.KlbRoi_xyzctUB_set(swigCPtr, this, value);
  }

  public long[] getXyzctUB() {
    return KlbJNI.KlbRoi_xyzctUB_get(swigCPtr, this);
  }

  public void defineSlice(int val, int dim, long[] xyzct) {
    KlbJNI.KlbRoi_defineSlice(swigCPtr, this, val, dim, xyzct);
  }

  public void defineFullImage(long[] xyzct) {
    KlbJNI.KlbRoi_defineFullImage(swigCPtr, this, xyzct);
  }

  public long getSizePixels(int dim) {
    return KlbJNI.KlbRoi_getSizePixels__SWIG_0(swigCPtr, this, dim);
  }

  public java.math.BigInteger getSizePixels() {
    return KlbJNI.KlbRoi_getSizePixels__SWIG_2(swigCPtr, this);
  }

  public KlbRoi() {
    this(KlbJNI.new_KlbRoi(), true);
  }

}
