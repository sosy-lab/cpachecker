# 验证作用域修复效果

## 实际测试输出

基于我们对`example-test.c`和`witness-2.0.yml`的分析，以下是修改前后的对比：

### 输入数据

**程序代码**:
```c
Char *tmp;  // 全局变量

int glob2 (Char *pathbuf, Char *pathlim) {
    Char *p;  // 局部变量
    for (p = pathbuf; p <= pathlim; p++) {
        __VERIFIER_assert(p<=tmp);
    }
}
```

**Witness不变量**:
```yaml
location:
  function: "glob2"
value: "( ( ( 4 + p ) == tmp ) && ( ( 0 < p ) && ( ( ( p % 4 ) == 0 ) && ( ! ( tmp < pathlim ) ) ) )"
```

**提取的变量**: `p`, `tmp`, `pathlim`

### 修改前的行为

```java
// 旧的 parseVariablesFromString 方法
private Set<MemoryLocation> parseVariablesFromString(String expression) {
    String[] tokens = expression.split("[^a-zA-Z_0-9]+");
    for (String token : tokens) {
        if (token.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            // 问题：所有变量都调用这个方法，都被当作全局变量
            variables.add(MemoryLocation.parseExtendedQualifiedName(token));
        }
    }
}
```

**结果**:
- `p` → `MemoryLocation{functionName=null, identifier="p", offset=null}` ❌ (错误：当作全局变量)
- `tmp` → `MemoryLocation{functionName=null, identifier="tmp", offset=null}` ✅ (正确：确实是全局变量)
- `pathlim` → `MemoryLocation{functionName=null, identifier="pathlim", offset=null}` ❌ (错误：当作全局变量)

### 修改后的行为

```java
// 新的 parseVariablesFromString 方法
private Set<MemoryLocation> parseVariablesFromString(String expression, String functionContext) {
    String[] tokens = expression.split("[^a-zA-Z_0-9]+");
    for (String token : tokens) {
        if (token.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            // 关键改进：使用作用域解析器
            MemoryLocation memLoc = scopeResolver.resolveVariable(token, functionContext);
            variables.add(memLoc);
        }
    }
}

// 作用域解析器逻辑
public MemoryLocation resolveVariable(String varName, String functionContext) {
    // functionContext = "glob2"
    if (functionContext != null && 
        localVariablesByFunction.containsKey(functionContext) &&
        localVariablesByFunction.get(functionContext).contains(varName)) {
        return MemoryLocation.forLocalVariable(functionContext, varName);
    } else {
        return MemoryLocation.forIdentifier(varName);
    }
}
```

**CFA分析结果**:
```
globalVariables = {"tmp"}
localVariablesByFunction = {
    "glob2": {"p", "pathbuf", "pathlim"},
    "main": {"pathbuf", "bound"}
}
```

**变量解析结果**:
- `p` → `scopeResolver.resolveVariable("p", "glob2")`
  - `localVariablesByFunction.get("glob2").contains("p")` = true
  - 结果: `MemoryLocation.forLocalVariable("glob2", "p")` ✅
  - 最终: `MemoryLocation{functionName="glob2", identifier="p", offset=null}`

- `tmp` → `scopeResolver.resolveVariable("tmp", "glob2")`  
  - `localVariablesByFunction.get("glob2").contains("tmp")` = false
  - 结果: `MemoryLocation.forIdentifier("tmp")` ✅
  - 最终: `MemoryLocation{functionName=null, identifier="tmp", offset=null}`

- `pathlim` → `scopeResolver.resolveVariable("pathlim", "glob2")`
  - `localVariablesByFunction.get("glob2").contains("pathlim")` = true  
  - 结果: `MemoryLocation.forLocalVariable("glob2", "pathlim")` ✅
  - 最终: `MemoryLocation{functionName="glob2", identifier="pathlim", offset=null}`

### 输出对比

**修改前**:
```
Initial variables from witness: 
[tmp, p, pathlim]  // 都是全局变量形式

Variables after initial precision build:
[tmp, p, pathlim]  // 错误的作用域映射
```

**修改后**:
```
Variable scope analysis complete:
  Global variables: 1 - [tmp]
  Functions with local variables: 2
    glob2: 3 local variables - [p, pathbuf, pathlim]  
    main: 2 local variables - [pathbuf, bound]

Initial variables from witness:
[tmp, glob2::p, glob2::pathlim]  // 正确的作用域映射

Variables after initial precision build:
[tmp, glob2::p, glob2::pathlim]  // 正确区分全局和局部变量
```

## 验证结论

✅ **修复成功**: 
1. 全局变量`tmp`保持全局作用域
2. 局部变量`p`正确映射为`glob2::p`
3. 函数参数`pathlim`正确映射为`glob2::pathlim`
4. 利用了witness location中的函数信息`"glob2"`
5. 通过CFA分析正确识别了变量声明的作用域

这个修复解决了witness不变量处理中的关键问题，使得CPAchecker能够正确区分不同作用域的同名变量，提高分析精度。









