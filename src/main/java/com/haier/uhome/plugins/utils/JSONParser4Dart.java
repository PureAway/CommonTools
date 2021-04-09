package com.haier.uhome.plugins.utils;

import com.haier.uhome.plugins.generate.DartClassGenerator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class JSONParser4Dart {
    private Stack<String> path = new Stack<>();
    private List<String> allNodes = new ArrayList<>();
    private DartClassGenerator dartClassGenerator;

    public void reset(Project project, PsiDirectory dir) {
        path.clear();
        allNodes.clear();
        dartClassGenerator = new DartClassGenerator(project, dir);
    }

    public void init(String mainClassName) {
        push(suffixToUppercase(mainClassName));
    }


    public String decodeJSONObject(JSONObject json) {
        String className;
        Iterator<String> keys = json.keys();
        JSONObject current;
        Object value;
        String key;
        className = dartClassGenerator.preGen(path.peek());
        if (null == className) {
            return null;
        }
        while (keys.hasNext()) {
            key = keys.next();
            value = json.get(key);
            key = Utils.getClassName(key);
            if (value instanceof JSONObject) {
                String validName = Utils.getClassName(suffixToUppercase(key));
                append(validName + " " + key + ";\n");
                push(validName);
                current = (JSONObject) value;
                if (current.keySet().size() > 0) {
                    decodeJSONObject(current);
                } else {
                    generateEmptyClass(false);
                    path.pop();
                }
            } else if (value instanceof JSONArray) {
                JSONArray v = (JSONArray) value;
                if (v.size() > 0 && !(v.get(0) instanceof JSONObject)) {
                    Object firstValue = v.get(0);
                    //处理基本数据类型数组和String数组
                    String field = getArrayType(decisionValueType(firstValue, true)) + " " + key + ";\n";
                    append(field);
                } else {
                    //处理对象数组
                    append("List<" + suffixToUppercase(key) + "Item>" + key + ";\n");
                }
                push(suffixToUppercase(key));
                decodeJSONArray((JSONArray) value);
            } else {
                //处理基本数据类型和String
                String field = "";
                field += decisionValueType(value, false) + " " + key + ";";
                append(field);
            }
        }
        if (!path.isEmpty()) {
            path.pop();
        }
        return className;
    }

    private void generateEmptyClass(boolean isArray) {
        String lasted = "";
        if (path.size() > 1) {
            lasted = path.get(path.size() - 2);
        }
        String name = path.peek();
        dartClassGenerator.preGen(isArray ? name + "Item" : name);
        append("// TODO: complemented needed maybe.");
        Utils.showInfo("Success to generating file " + path.peek() + ".java but it have no field");
    }


    private String decisionValueType(Object value, boolean formArray) {
        if (formArray) {
            return value.getClass().getSimpleName();
        } else {
            if (value instanceof Integer) {
                return "int";
            } else if (value instanceof Long) {
                return "long";
            } else if (value instanceof Double) {
                return "double";
            } else if (value instanceof Boolean) {
                return "boolean";
            }
        }
        return "String";
    }


    private String getArrayType(String baseType) {
        return "List<" + baseType + ">";
    }

    private void decodeJSONArray(JSONArray jsonArray) {
        if (jsonArray.toArray().length == 0) {
            generateEmptyClass(true);
            if (!path.isEmpty()) {
                path.pop();
            }
            return;
        }
        Object item = jsonArray.get(0);
        if (item instanceof JSONObject) {
            push(path.peek() + "Item");
            decodeJSONObject((JSONObject) item);
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

    public void append(String field) {
        String name = path.peek();
        dartClassGenerator.append(field);
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
