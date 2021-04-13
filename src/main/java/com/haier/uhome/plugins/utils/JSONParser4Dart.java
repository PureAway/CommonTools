package com.haier.uhome.plugins.utils;

import com.haier.uhome.plugins.generate.DartClassGenerator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.*;

public class JSONParser4Dart {
    private final Stack<String> path = new Stack<>();
    private final List<String> allNodes = new ArrayList<>();
    private DartClassGenerator dartClassGenerator;
    private String className = null;
    private final List<StringBuilder> builders = new ArrayList<>();

    public void reset(Project project, PsiDirectory dir) {
        path.clear();
        allNodes.clear();
        builders.clear();
        dartClassGenerator = new DartClassGenerator(project, dir);
    }

    public void init(String mainClassName) {
        push(suffixToUppercase(mainClassName));
        className = dartClassGenerator.preGen(path.peek());
    }


    public String decodeJSONObject(JSONObject json, boolean mainClass) {
        Iterator<String> keys = json.keys();
        JSONObject current;
        Object value;
        String key;
        if (null == className) {
            return null;
        }
        String name = path.peek();
        String beginValue = mainClass ? "" : "\n\n";
        StringBuilder stringBuilder = new StringBuilder(beginValue + "@JsonSerializable()\n");
        stringBuilder.append("class ")
                .append(name)
                .append(" {\n");

        StringBuilder fieldBuilder = new StringBuilder("\n  " + name);
        fieldBuilder.append("(\n");
        while (keys.hasNext()) {
            key = keys.next();
            value = json.get(key);
            key = Utils.getClassName(key);
            fieldBuilder.append("      this.").append(key).append(",\n");
            if (value instanceof JSONObject) {
                String validName = Utils.getClassName(suffixToUppercase(key));
                stringBuilder.append("  ")
                        .append(validName)
                        .append(" ")
                        .append(key)
                        .append(";\n");
                push(validName);
                current = (JSONObject) value;
                if (current.keySet().size() > 0) {
                    decodeJSONObject(current, false);
                } else {
                    generateEmptyClass();
                    path.pop();
                }
            } else if (value instanceof JSONArray) {
                JSONArray v = (JSONArray) value;
                if (v.size() > 0 && !(v.get(0) instanceof JSONObject)) {
                    Object firstValue = v.get(0);
                    //处理基本数据类型数组和String数组
                    String field = getArrayType(decisionValueType(firstValue, true)) + " " + key + ";\n";
                    stringBuilder.append(field);
                } else {
                    //处理对象数组
                    stringBuilder.append("  List<")
                            .append(suffixToUppercase(key))
                            .append("Item>")
                            .append(key)
                            .append(";\n");
                }
                push(suffixToUppercase(key));
                decodeJSONArray((JSONArray) value);
            } else {
                //处理基本数据类型和String
                String field = "  ";
                field += decisionValueType(value, false) + " " + key + ";\n";
                stringBuilder.append(field);
            }
        }
        int index = fieldBuilder.indexOf("this.", 0);
        fieldBuilder.replace(index, index + 5, "{this.");
        fieldBuilder.deleteCharAt(fieldBuilder.lastIndexOf(","));
        fieldBuilder.deleteCharAt(fieldBuilder.lastIndexOf("\n"));
        fieldBuilder.append("});\n\n");
        fieldBuilder.append("  factory ")
                .append(name)
                .append(".fromJson(Map<String, dynamic> json) =>\n")
                .append("      _$")
                .append(name)
                .append("FromJson(json);\n\n");
        fieldBuilder.append("  Map<String, dynamic> toJson() => _$")
                .append(name)
                .append("ToJson(this);")
                .append("\n}\n");
        stringBuilder.append(fieldBuilder.toString());
        if (!path.isEmpty()) {
            builders.add(stringBuilder);
            path.pop();
            if (path.isEmpty() && !builders.isEmpty()) {
                StringBuilder resultString = new StringBuilder();
                for (StringBuilder builder : builders) {
                    resultString.append(builder.toString());
                }
                dartClassGenerator.append(resultString.toString());
            }
        }
        return className;
    }

    private void generateEmptyClass() {
        String name = path.peek();
        StringBuilder stringBuilder = new StringBuilder("@JsonSerializable()\n");
        stringBuilder.append("class ")
                .append(name)
                .append(" {\n")
                .append("  // TODO: complemented needed maybe.\n}\n");
        builders.add(stringBuilder);
        Utils.showInfo("Success to generating class " + name + ", but it have no field");
    }


    private String decisionValueType(Object value, boolean formArray) {
        if (formArray) {
            return value.getClass().getSimpleName();
        } else {
            if (value instanceof Integer) {
                return "int";
            } else if (value instanceof Long) {
                return "int";
            } else if (value instanceof Double) {
                return "double";
            } else if (value instanceof Boolean) {
                return "bool";
            }
        }
        return "String";
    }


    private String getArrayType(String baseType) {
        return "  List<" + baseType + ">";
    }

    private void decodeJSONArray(JSONArray jsonArray) {
        if (jsonArray.toArray().length == 0) {
            generateEmptyClass();
            if (!path.isEmpty()) {
                path.pop();
            }
            return;
        }
        Object item = jsonArray.get(0);
        if (item instanceof JSONObject) {
            push(path.peek() + "Item");
            decodeJSONObject((JSONObject) item, false);
        } else if (item instanceof JSONArray) {
            push(path.peek() + "Item");
            decodeJSONArray((JSONArray) item);
        }
        if (!path.isEmpty()) {
            path.pop();
        }
    }

    public String suffixToUppercase(String s) {
        StringBuilder sb = new StringBuilder(s);
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        return sb.toString();
    }

    private void push(String name) {
        String uniqueName = Utils.getClassName(name);
        if (allNodes.contains(name)) {
            uniqueName = path.peek() + name;
        }

        if (allNodes.contains(uniqueName)) {
            for (int i = 1; i <= 50; i++) {
                uniqueName = uniqueName + i;
                if (!allNodes.contains(uniqueName)) {
                    break;
                }
            }
        }
        allNodes.add(uniqueName);
        path.push(uniqueName);
    }

}
