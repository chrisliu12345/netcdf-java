/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.core.dmr;

import java.nio.ByteBuffer;
import static dap4.core.dmr.TypeConstants.*;

/*
 * This private constants class
 * is necessary because enum classes
 * will not allow anything to be defined
 * before the enum constants.
 */
abstract class TypeConstants {
  // define classification flag mnemonics
  public static final int ISINTEGER = (1 << 0);
  public static final int ISFLOAT = (1 << 1);
  public static final int ISSTRING = (1 << 2);
  public static final int ISCHAR = (1 << 3);
  public static final int ISOPAQUE = (1 << 4);
  public static final int ISUNSIGNED = (1 << 5);
  public static final int ISFIXEDSIZE = (1 << 6);
  public static final int ISENUM = (1 << 7);
  public static final int ISSTRUCT = (1 << 8);
  public static final int ISSEQ = (1 << 9);
  public static final int ISATOMIC = (1 << 10);
  public static final int ISCOMPOUND = (1 << 11);
  public static final int ISLEGALATTRTYPE = (1 << 12);
}


public enum TypeSort {
  Char("Char", char.class, ISFIXEDSIZE | ISCHAR | ISLEGALATTRTYPE | ISATOMIC), Int8("Int8", byte.class,
      ISINTEGER | ISFIXEDSIZE | ISLEGALATTRTYPE | ISATOMIC), UInt8("UInt8", byte.class,
          ISINTEGER | ISUNSIGNED | ISFIXEDSIZE | ISLEGALATTRTYPE | ISATOMIC), Int16("Int16", short.class,
              ISINTEGER | ISFIXEDSIZE | ISLEGALATTRTYPE | ISATOMIC), UInt16("UInt16", short.class,
                  ISINTEGER | ISUNSIGNED | ISFIXEDSIZE | ISLEGALATTRTYPE | ISATOMIC), Int32("Int32", int.class,
                      ISINTEGER | ISFIXEDSIZE | ISLEGALATTRTYPE | ISATOMIC), UInt32("UInt32", int.class,
                          ISINTEGER | ISUNSIGNED | ISFIXEDSIZE | ISLEGALATTRTYPE | ISATOMIC), Int64("Int64", long.class,
                              ISINTEGER | ISFIXEDSIZE | ISLEGALATTRTYPE | ISATOMIC), UInt64("UInt64", long.class,
                                  ISINTEGER | ISUNSIGNED | ISFIXEDSIZE | ISLEGALATTRTYPE | ISATOMIC), Float32("Float32",
                                      float.class, ISFLOAT | ISFIXEDSIZE | ISLEGALATTRTYPE | ISATOMIC), Float64(
                                          "Float64", double.class,
                                          ISFLOAT | ISFIXEDSIZE | ISLEGALATTRTYPE | ISATOMIC), String("String",
                                              String.class, ISSTRING | ISLEGALATTRTYPE | ISATOMIC), URL("URL",
                                                  String.class, ISSTRING | ISLEGALATTRTYPE | ISATOMIC), Opaque("Opaque",
                                                      ByteBuffer.class,
                                                      ISOPAQUE | ISLEGALATTRTYPE | ISATOMIC), Enum("Enum", null,
                                                          ISENUM | ISATOMIC | ISFIXEDSIZE | ISLEGALATTRTYPE), Structure(
                                                              "Structure", null, ISSTRUCT | ISCOMPOUND), // Add this to
                                                                                                         // avoid having
                                                                                                         // to check the
                                                                                                         // DapSort
  Sequence("Sequence", null, ISSEQ | ISCOMPOUND); // Add this to avoid having to check the DapSort

  private final String typename;
  private final int classification;
  private final Class javaclass;

  TypeSort(String typename, Class javaclass, int classification) {
    this.typename = typename; // Must match xml variable open name
    this.classification = classification;
    this.javaclass = javaclass;
  }

  public final String getTypeName() {
    return typename;
  }

  public final Class getJavaClass() {
    return javaclass;
  }

  public static final TypeSort getSignedVersion(TypeSort uat) {
    switch (uat) {
      case Int8:
      case UInt8:
        return Int8;
      case Int16:
      case UInt16:
        return Int16;
      case Int32:
      case UInt32:
        return Int32;
      case Int64:
      case UInt64:
        return Int64;
      default:
        break;
    }
    return null;
  }

  public static final int getSize(TypeSort uat) {
    switch (uat) {
      case Char:
      case Int8:
      case UInt8:
        return 1;
      case Int16:
      case UInt16:
        return 2;
      case Int32:
      case UInt32:
      case Float32:
        return 4;
      case Int64:
      case UInt64:
      case Float64:
        return 8;
      default:
        break;
    }
    return 0;
  }

  // Map String -> TypeSort
  public static TypeSort getTypeSort(String typename) {
    for (TypeSort dt : TypeSort.values()) {
      if (typename.equalsIgnoreCase(dt.getTypeName()))
        return dt;
    }
    return null;
  }

  // Specific classifier tests
  public boolean isIntegerType() {
    return (classification & ISINTEGER) != 0;
  }

  public boolean isFloatType() {
    return (classification & ISFLOAT) != 0;
  }

  public boolean isStringType() {
    return (classification & ISSTRING) != 0;
  }

  public boolean isCharType() {
    return (classification & ISCHAR) != 0;
  }

  public boolean isEnumType() {
    return (classification & ISENUM) != 0;
  }

  public boolean isOpaqueType() {
    return (classification & ISOPAQUE) != 0;
  }

  public boolean isStructType() {
    return (classification & ISSTRUCT) != 0;
  }

  public boolean isSeqType() {
    return (classification & ISSEQ) != 0;
  }

  public boolean isCompoundType() {
    return (classification & ISSTRUCT) != 0 || (classification & ISSEQ) != 0;
  }

  public boolean isNumericType() {
    return (classification & (ISINTEGER | ISFLOAT)) != 0;
  }

  public boolean isUnsigned() {
    return (classification & ISUNSIGNED) != 0;
  }

  public boolean isFixedSize() {
    return (classification & ISFIXEDSIZE) != 0;
  }

  public boolean isLegalAttrType() {
    return (classification & ISLEGALATTRTYPE) != 0;
  }

  public boolean isCompound() {
    return (classification & ISCOMPOUND) != 0;
  }

  public boolean isAtomic() {
    return (classification & ISATOMIC) != 0;
  }


  /*
   * // Map an object instance class to a corresponding TypeSort
   * // Try to cover as many Java classes as possible.
   * // Notes:
   * // 1. signedness is ignored
   * 
   * static Map<Class, TypeSort> classmap;
   * static {
   * classmap = new HashMap<Class, TypeSort>();
   * classmap.put(Character.class, TypeSort.Char);
   * classmap.put(Byte.class, TypeSort.Int8);
   * classmap.put(Short.class, TypeSort.Int16);
   * classmap.put(Integer.class, TypeSort.Int32);
   * classmap.put(Long.class, TypeSort.Int64);
   * classmap.put(Float.class, TypeSort.Float32);
   * classmap.put(Double.class, TypeSort.Float64);
   * classmap.put(String.class, TypeSort.String);
   * classmap.put(URI.class, TypeSort.URL);
   * classmap.put(URL.class, TypeSort.URL);
   * classmap.put(ByteBuffer.class, TypeSort.Opaque);
   * classmap.put(byte[].class, TypeSort.Opaque);
   * classmap.put(BigInteger.class, TypeSort.UInt64);
   * classmap.put(DapEnumeration.class, TypeSort.Enum);
   * }
   * // Convert the class of an object to a matching TypeSort
   * static public TypeSort classToType(Object o)
   * {
   * if(o == null) return null;
   * return classmap.get(o.getClass());
   * }
   */

}


