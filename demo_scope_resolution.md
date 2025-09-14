# CPAchecker Witness变量作用域解析修复演示

## 问题描述

在原来的`WitnessToValuePrecisionConverter`中，从witness不变量提取变量时存在作用域映射问题：
- 所有变量都被当作全局变量处理
- 没有利用witness location中的函数信息
- 无法正确区分局部变量和全局变量

## 解决方案

我们实现了以下修改：

### 1. 创建了VariableScopeResolver类

```java
private static class VariableScopeResolver {
    private final Map<String, Set<String>> localVariablesByFunction;
    private final Set<String> globalVariables;
    
    // 从CFA中提取所有变量声明并按作用域分类
    private void extractVariableDeclarationsFromCFA(CFA cfa) {
        // 遍历所有CFA边，提取变量声明
        // 根据isGlobal()判断变量作用域
        // 按函数分组收集局部变量
    }
    
    // 根据函数上下文解析变量的正确MemoryLocation
    public MemoryLocation resolveVariable(String varName, String functionContext) {
        if (functionContext != null && 
            localVariablesByFunction.containsKey(functionContext) &&
            localVariablesByFunction.get(functionContext).contains(varName)) {
            return MemoryLocation.forLocalVariable(functionContext, varName);
        } else {
            return MemoryLocation.forIdentifier(varName);  // 全局变量
        }
    }
}
```

### 2. 修改了变量解析流程

```java
// 原来的错误实现
private Set<MemoryLocation> parseVariablesFromString(String expression) {
    // 直接调用parseExtendedQualifiedName，都当作全局变量
    variables.add(MemoryLocation.parseExtendedQualifiedName(token));
}

// 修改后的正确实现
private Set<MemoryLocation> parseVariablesFromString(String expression, String functionContext) {
    // 使用作用域解析器进行正确的变量解析
    MemoryLocation memLoc = scopeResolver.resolveVariable(token, functionContext);
    variables.add(memLoc);
}
```

### 3. 传递witness location的函数信息

```java
public Set<MemoryLocation> extractVariablesFromInvariant(WitnessInvariant invariant) {
    // 提取witness location中的函数信息
    String functionContext = null;
    if (invariant.getLocation() != null) {
        functionContext = invariant.getLocation().getFunction();
    }
    
    // 传递函数上下文给变量解析器
    variables.addAll(parseVariablesFromString(expression, functionContext));
}
```

## 测试案例

以`doc/examples/example-test.c`为例：

```c
Char *tmp;  // 全局变量

int glob2 (Char *pathbuf, Char *pathlim) {  // 函数参数
    Char *p;  // 局部变量
    
    for (p = pathbuf; p <= pathlim; p++) {
        // witness中的不变量可能包含: p, pathbuf, pathlim, tmp
    }
}
```

在witness文件中：
```yaml
- invariant:
    location:
      function: "glob2"  # 函数上下文
    value: "( ( 4 + p ) == tmp )"  # 包含局部变量p和全局变量tmp
```

## 修改效果

**修改前**：
- `p` → `MemoryLocation.forIdentifier("p")` (错误：当作全局变量)
- `tmp` → `MemoryLocation.forIdentifier("tmp")` (正确：本来就是全局变量)

**修改后**：
- `p` → `MemoryLocation.forLocalVariable("glob2", "p")` (正确：局部变量)
- `tmp` → `MemoryLocation.forIdentifier("tmp")` (正确：全局变量)

## 技术要点

1. **CFA信息利用**：通过遍历`CFAUtils.allEdges(cfa)`和`cfa.entryNodes()`提取变量声明
2. **作用域判断**：使用`varDecl.isGlobal()`和函数上下文信息
3. **正确的MemoryLocation创建**：
   - 局部变量：`MemoryLocation.forLocalVariable(functionName, varName)`
   - 全局变量：`MemoryLocation.forIdentifier(varName)`
4. **Witness信息利用**：使用`invariant.getLocation().getFunction()`获取函数上下文

这样修改后，从witness不变量中提取的变量将会正确映射到程序中的实际变量作用域，提高值分析的精度。
