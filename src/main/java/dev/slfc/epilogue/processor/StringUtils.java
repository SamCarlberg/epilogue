package dev.slfc.epilogue.processor;

public class StringUtils {
  public static String simpleName(String fqn) {
    return fqn.substring(fqn.lastIndexOf('.') + 1);
  }

  public static String upperCamelCase(CharSequence str) {
    return Character.toUpperCase(str.charAt(0)) + str.subSequence(1, str.length()).toString();
  }

  public static String lowerCamelCase(CharSequence str) {
    StringBuilder builder = new StringBuilder(str.length());

    int i = 0;
    for (; i < str.length() - 1 && (i == 0 || (Character.isUpperCase(str.charAt(i)) && Character.isUpperCase(str.charAt(i + 1)))); i++) {
      builder.append(Character.toLowerCase(str.charAt(i)));
    }

    builder.append(str.subSequence(i, str.length()));
    return builder.toString();
  }
}
