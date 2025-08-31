package org.lucentrix.metaframe.plugin.solr;


public class SolrDynamicField implements Comparable<SolrDynamicField> {
    abstract protected static class DynamicPattern {
        protected final String regex;
        protected final String fixedStr;

        protected DynamicPattern(String regex, String fixedStr) { this.regex = regex; this.fixedStr = fixedStr; }

        static DynamicPattern createPattern(String regex) {
            if (regex.startsWith("*")) {
                return new NameEndsWith(regex);
            } else if (regex.endsWith("*")) {
                return new NameStartsWith(regex);
            } else if (regex.contains("*")) {
                return new NameMatches(regex);
            } else {
                return new NameEquals(regex);
            }
        }

        public String getRegex() {
            return regex;
        }

        /** Returns true if the given name matches this pattern */
        abstract boolean matches(String name);

        /** Returns the remainder of the given name after removing this pattern's fixed string component */
        abstract String remainder(String name);

        /** Returns the result of combining this pattern's fixed string component with the given replacement */
        abstract String subst(String replacement);

        /** Returns the length of the original regex, including the asterisk, if any. */
        public int length() { return regex.length(); }

        private static class NameStartsWith extends DynamicPattern {

            NameStartsWith(String regex) {
                super(regex, regex.substring(0, regex.length() - 1));
            }

            boolean matches(String name) {
                return name.startsWith(fixedStr);
            }

            String remainder(String name) {
                return name.substring(fixedStr.length());
            }

            String subst(String replacement) {
                return fixedStr + replacement;
            }
        }

        private static class NameEndsWith extends DynamicPattern {

            NameEndsWith(String regex) {
                super(regex, regex.substring(1));
            }

            boolean matches(String name) {
                return name.endsWith(fixedStr);
            }

            String remainder(String name) {
                return name.substring(0, name.length() - fixedStr.length());
            }

            String subst(String replacement) {
                return replacement + fixedStr;
            }
        }

        private static class NameMatches extends DynamicPattern {

            NameMatches(String regex) {
                super(regex, regex);
            }

            boolean matches(String name) {
                return name.matches(regex);
            }

            String remainder(String name) {
                return name;
            }

            String subst(String replacement) {
                return replacement;
            }
        }

        static class NameEquals extends DynamicPattern {

            NameEquals(String regex) {
                super(regex, regex);
            }

            boolean matches(String name) {
                return regex.equals(name);
            }

            String remainder(String name) {
                return "";
            }

            String subst(String replacement) {
                return fixedStr;
            }
        }

        @Override
        public String toString() {
            return "DynamicPattern{" +
                    "regex='" + regex + '\'' +
                    ", fixedStr='" + fixedStr + '\'' +
                    '}';
        }
    }

    protected final DynamicPattern pattern;

    public boolean matches(String name) { return pattern.matches(name); }

    public SolrDynamicField(String regex) {
        pattern = DynamicPattern.createPattern(regex);
    }

    public DynamicPattern getPattern() {
        return pattern;
    }

    /**
     * Sort order is based on length of regex.  Longest comes first.
     * @param other The object to compare to.
     * @return a negative integer, zero, or a positive integer
     * as this object is less than, equal to, or greater than
     * the specified object.
     */
    @Override
    public int compareTo(SolrDynamicField other) {
        return other.pattern.length() - pattern.length();
    }

    /** Returns the regex used to create this instance's pattern */
    public String getRegex() {
        return pattern.regex;
    }

    @Override
    public String toString() {
        return "SolrDynamicField{" +
                "pattern=" + pattern +
                '}';
    }
}