package com.zcy.plugins.utils

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.zcy.plugins.generate.DartClassGenerator
import com.zcy.plugins.utils.Utils.getClassName
import com.zcy.plugins.utils.Utils.showInfo
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import java.util.*

class JSONParser4Dart {
    private val path = Stack<String>()
    private val allNodes: MutableList<String> = ArrayList()
    private var dartClassGenerator: DartClassGenerator? = null
    private var className: String? = null
    private val builders: MutableList<StringBuilder> = ArrayList()
    fun reset(project: Project?, dir: PsiDirectory?) {
        path.clear()
        allNodes.clear()
        builders.clear()
        dartClassGenerator = DartClassGenerator(project!!, dir!!)
    }

    fun init(mainClassName: String?) {
        push(suffixToUppercase(mainClassName))
        className = dartClassGenerator!!.preGen(path.peek())
    }

    fun decodeJSONObject(json: JSONObject, mainClass: Boolean): String? {
        val keys: Iterator<String?> = json.keys() as Iterator<String?>
        var current: JSONObject
        var value: Any?
        var key: String?
        if (null == className) {
            return null
        }
        val name = path.peek()
        val beginValue = if (mainClass) "" else "\n"
        val stringBuilder = StringBuilder("$beginValue@JsonSerializable()\n")
        stringBuilder.append("class ")
            .append(name)
            .append(" {\n")
        val fieldBuilder = StringBuilder("\n  $name")
        fieldBuilder.append("(\n")
        while (keys.hasNext()) {
            key = keys.next()
            value = json[key]
            key = getClassName(key!!)
            fieldBuilder.append("      this.").append(key).append(",\n")
            if (value is JSONObject) {
                val validName = getClassName(suffixToUppercase(key))
                stringBuilder.append("  ")
                    .append(validName)
                    .append(" ")
                    .append(key)
                    .append(";\n")
                push(validName)
                current = value
                if (current.keys.size > 0) {
                    decodeJSONObject(current, false)
                } else {
                    generateEmptyClass()
                    path.pop()
                }
            } else if (value is JSONArray) {
                val v = value
                if (v.size > 0 && v[0] !is JSONObject) {
                    val firstValue = v[0]
                    //处理基本数据类型数组和String数组
                    val field = """${getArrayType(decisionValueType(firstValue!!, true))} $key;
"""
                    stringBuilder.append(field)
                } else {
                    //处理对象数组
                    stringBuilder.append("  List<")
                        .append(suffixToUppercase(key))
                        .append("Item>")
                        .append(key)
                        .append(";\n")
                }
                push(suffixToUppercase(key))
                decodeJSONArray(value)
            } else {
                //处理基本数据类型和String
                var field = "  "
                field += """${decisionValueType(value, false)} $key;
"""
                stringBuilder.append(field)
            }
        }
        val index = fieldBuilder.indexOf("this.", 0)
        fieldBuilder.replace(index, index + 5, "{this.")
        fieldBuilder.deleteCharAt(fieldBuilder.lastIndexOf(","))
        fieldBuilder.deleteCharAt(fieldBuilder.lastIndexOf("\n"))
        fieldBuilder.append("});\n\n")
        fieldBuilder.append("  factory ")
            .append(name)
            .append(".fromJson(Map<String, dynamic> json) =>\n")
            .append("      _$")
            .append(name)
            .append("FromJson(json);\n\n")
        fieldBuilder.append("  Map<String, dynamic> toJson() => _$")
            .append(name)
            .append("ToJson(this);")
            .append("\n}\n")
        stringBuilder.append(fieldBuilder.toString())
        if (!path.isEmpty()) {
            builders.add(stringBuilder)
            path.pop()
            if (path.isEmpty() && !builders.isEmpty()) {
                val lasted = builders.removeAt(builders.size - 1).toString()
                val resultString = StringBuilder(lasted)
                for (builder in builders) {
                    resultString.append(builder.toString())
                }
                dartClassGenerator!!.append(resultString.toString())
            }
        }
        return className
    }

    private fun generateEmptyClass() {
        val name = path.peek()
        val stringBuilder = StringBuilder("\n")
        stringBuilder.append("class ")
            .append(name)
            .append(" {\n")
            .append("  // TODO: complemented needed maybe.\n}\n")
        builders.add(stringBuilder)
        showInfo("Success to generating class $name, but it have no field")
    }

    private fun decisionValueType(value: Any, formArray: Boolean): String {
        if (formArray) {
            return value.javaClass.simpleName
        } else {
            when (value) {
                is Int -> {
                    return "int"
                }
                is Long -> {
                    return "int"
                }
                is Double -> {
                    return "double"
                }
                is Boolean -> {
                    return "bool"
                }
            }
        }
        return "String"
    }

    private fun getArrayType(baseType: String): String {
        return "  List<$baseType>"
    }

    private fun decodeJSONArray(jsonArray: JSONArray) {
        if (jsonArray.toTypedArray().size == 0) {
            generateEmptyClass()
            if (!path.isEmpty()) {
                path.pop()
            }
            return
        }
        val item = jsonArray[0]
        if (item is JSONObject) {
            push(path.peek().toString() + "Item")
            decodeJSONObject(item, false)
        } else if (item is JSONArray) {
            push(path.peek().toString() + "Item")
            decodeJSONArray(item)
        }
        if (!path.isEmpty()) {
            path.pop()
        }
    }

    fun suffixToUppercase(s: String?): String {
        val sb = StringBuilder(s)
        sb.setCharAt(0, Character.toUpperCase(sb[0]))
        return sb.toString()
    }

    private fun push(name: String) {
        var uniqueName = getClassName(name)
        if (allNodes.contains(name)) {
            uniqueName = path.peek().toString() + name
        }
        if (allNodes.contains(uniqueName)) {
            for (i in 1..50) {
                uniqueName = uniqueName + i
                if (!allNodes.contains(uniqueName)) {
                    break
                }
            }
        }
        allNodes.add(uniqueName)
        path.push(uniqueName)
    }
}