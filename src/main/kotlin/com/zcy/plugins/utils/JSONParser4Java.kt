package com.zcy.plugins.utils

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.zcy.plugins.generate.JavaClassGenerator
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import java.util.*

class JSONParser4Java {
    private val path = Stack<String>()
    private val allNodes: MutableList<String> = ArrayList()
    private lateinit var javaClassGenerator: JavaClassGenerator
    private var isArrayToList = true
    private var genGetter = false
    private var genSetter = false
    fun reset(project: Project, dir: PsiDirectory) {
        path.clear()
        allNodes.clear()
        javaClassGenerator = JavaClassGenerator(project, dir)
    }

    fun init(mainClassName: String?, isArrayToList: Boolean) {
        push(suffixToUppercase(mainClassName))
        this.isArrayToList = isArrayToList
    }

    fun decodeJSONObject(json: JSONObject): String {
        val className: String
        val keys: Iterator<String> = json.keys() as Iterator<String>
        var current: JSONObject
        var value: Any
        var key: String
        var last = ""
        if (path.size > 1) {
            last = path[path.size - 2]
        }
        className = javaClassGenerator.preGen(path.peek(), last)
        while (keys.hasNext()) {
            key = keys.next()
            value = json.get(key)
            key = Utils.getClassName(key)
            if (value is JSONObject) {
                val validName = Utils.getClassName(suffixToUppercase(key))
                val modifier = modifier
                append("$modifier$validName $key;\n", false)
                push(validName)
                current = value
                if (current.keys.size > 0) {
                    decodeJSONObject(current)
                } else {
                    generateEmptyClass(false)
                    path.pop()
                }
            } else if (value is JSONArray) {
                val v: JSONArray = value
                if (v.size > 0 && v[0] !is JSONObject) {
                    val firstValue: Any? = v[0]
                    firstValue?.let {
                        //处理基本数据类型数组和String数组
                        val field =
                            """$modifier${getArrayType(decisionValueType(firstValue, true), isArrayToList)} $key;
"""
                        append(field, false)
                    }
                } else {
                    //处理对象数组
                    if (isArrayToList) {
                        append(
                            """
    ${modifier}List<${suffixToUppercase(key)}Item>$key;
    
    """.trimIndent(), false
                        )
                    } else {
                        append(
                            """
    $modifier${suffixToUppercase(key)}Item[] $key;
    
    """.trimIndent(), false
                        )
                    }
                }
                push(suffixToUppercase(key))
                decodeJSONArray(value)
            } else {
                //处理基本数据类型和String
                var field = modifier
                field += decisionValueType(value, false) + " " + key + ";"
                append(field, false)
            }
        }
        if (!path.isEmpty()) {
            path.pop()
        }
        return className
    }

    private fun generateEmptyClass(isArray: Boolean) {
        var lasted = ""
        if (path.size > 1) {
            lasted = path[path.size - 2]
        }
        val name = path.peek()
        javaClassGenerator.preGen(if (isArray) name + "Item" else name, lasted)
        append("// TODO: complemented needed maybe.", isArray)
        Utils.showInfo("Success to generating file " + path.peek() + ".java but it have no field")
    }

    private val modifier: String
        get() = if (!genGetter && !genSetter) {
            "public "
        } else {
            "private "
        }

    private fun decisionValueType(value: Any, formArray: Boolean): String {
        if (formArray) {
            return value.javaClass.simpleName
        } else {
            if (value is Int) {
                return "int"
            } else if (value is Long) {
                return "long"
            } else if (value is Double) {
                return "double"
            } else if (value is Boolean) {
                return "boolean"
            }
        }
        return "String"
    }

    private fun getArrayType(baseType: String, isArrayToList: Boolean): String {
        return if (isArrayToList) {
            "List<$baseType>"
        } else {
            "$baseType[]"
        }
    }

    private fun decodeJSONArray(jsonArray: JSONArray) {
        if (jsonArray.toTypedArray().isEmpty()) {
            generateEmptyClass(true)
            if (!path.isEmpty()) {
                path.pop()
            }
            return
        }
        val item: Any? = jsonArray[0]
        if (item is JSONObject) {
            push(path.peek().toString() + "Item")
            decodeJSONObject(item)
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

    fun append(field: String, isArray: Boolean) {
        val name = path.peek()
        javaClassGenerator.append(field, if (isArray) name + "Item" else name)
    }

    private fun push(name: String) {
        var uniqueName = Utils.getClassName(name)
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

    fun setGenGetter(genGetter: Boolean) {
        this.genGetter = genGetter
        javaClassGenerator.setGenGetter(genGetter)
    }

    fun setGenSetter(genSetter: Boolean) {
        this.genSetter = genSetter
        javaClassGenerator.setGenSetter(genSetter)
    }
}