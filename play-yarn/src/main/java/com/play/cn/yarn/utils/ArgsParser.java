package com.play.cn.yarn.utils;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * ArgsParser.
 */
public class ArgsParser {

    private String[] args = null;

    private Map<String, HashSet<String>> kv = new HashMap();
    private Options options;

    /**
     * @param options options
     * @param arguments argument
     */
    public ArgsParser(Options options, String[] arguments) {
        parse(options, arguments);
    }

    /**
     * @param options options
     * @param arguments arguments
     * @return ArgsParser
     */
    private ArgsParser parse(Options options, String[] arguments) {
        this.options = options;
        this.args = arguments;
        kv.clear();
        String tmpValue = "";
        String key = "";
        tmpValue = "";
        for (int i = 0; i < args.length; i++) {
            if (isOptions(args[i])) {
                if (!StringUtils.isEmpty(key)) {
                    addKV(kv, key, tmpValue);
                    tmpValue = "";
                    key = "";
                }
                key = getOptionKeyFromStr(args[i], "");
            } else {
                tmpValue += " ";
                tmpValue += args[i];
            }

            if (args.length - 1 == i) {
                addKV(kv, key, tmpValue);
            }
        }
        return this;
    }

    private void addKV(Map<String, HashSet<String>> map, String key, String value) {
        if (!map.containsKey(key)) {
            HashSet<String> values = new HashSet();
            values.add(value.trim());
            map.put(key, values);
        } else {
            map.get(key).add(value.trim());
        }

    }

    /**
     * @param key key
     * @return value
     */
    public String getOption(String key) {
      return getOptionValue(key, null);
    }

    /**
     * @param key key
     * @return value
     */
    public Object[] getOptionValues(String key) {
        HashSet<String> x = kv.get(key);
        if (x == null) {
            return null;
        }
        return x.toArray();
    }

    /**
     * @param key key
     * @return true is contains, other is not
     */
    public boolean hasOption(String key) {
      return kv.get(key) != null;
    }

    /**
     * @param key key
     * @param def value
     * @return string
     */
    public String getOptionValue(String key, String def) {
        HashSet<String> result = kv.get(key);
        if (result == null) {
            return def;
        }
        if (result.isEmpty()) {
            return def;
        }
        return result.iterator().next();
    }

    private String getOptionKeyFromStr(String str, String def) {
        int pos = str.lastIndexOf('-');
        if (pos < 0) {
            return def;
        }

        if ((pos + 1) < str.length()) {
            return str.substring(pos + 1);
        }
        return str.substring(pos);
    }

    private boolean isOptionRule(String str) {
        String tmp = getOptionKeyFromStr(str, null);
        //System.out.println("get key " + str + "   ====" + tmp);
        if (tmp != null) {
            return options.hasOption(tmp);
        }
        return false;
    }

    private boolean isOptions(String str) {
        if (str.startsWith("-") && isOptionRule(str)) {
          return true;
        }
        return false;
    }

    /**
     * @return string
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        for (Map.Entry<String, HashSet<String>> entity : kv.entrySet()) {
            for (String str : entity.getValue()) {
                sb.append("<").append(entity.getKey()).append("=").append(str).append(">\n");
            }
        }
        return sb.toString();
    }

    public static void printUsage(Options opts, String description) {
        new HelpFormatter().printHelp(description, opts);
    }
}