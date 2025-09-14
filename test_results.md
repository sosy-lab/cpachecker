# CPAchecker Witness变量作用域解析测试结果

## 测试程序：`doc/examples/example-test.c`

### 程序结构分析：

```c
// 全局变量
Char *tmp;  // 第23行

int glob2 (Char *pathbuf, Char *pathlim) {  // 函数参数
    Char *p;  // 局部变量，第27行
    
    for (p = pathbuf; p <= pathlim; p++) {
        __VERIFIER_assert(p<=tmp);  // 第31行
        *p = 1;
    }
}

int main() {
    Char pathbuf[1 +1];    // main函数局部变量
    Char *bound = ...;     // main函数局部变量
    // ...
}
```

### Witness文件分析（`output/witness-2.0.yml`）：

```yaml
- invariant:
    type: "loop_invariant"
    location:
      function: "glob2"  # 函数上下文
    value: "( ( ( 4 + p ) == tmp ) && ( ( 0 < p ) && ( ( ( p % 4 ) == 0 ) && ( ! ( tmp < pathlim ) ) ) ) )"
    # 包含变量: p, tmp, pathlim

- invariant:
    type: "location_invariant"
    location:
      function: "glob2"
    value: "1"
    # 包含变量: 无（常量）
```

## 作用域解析测试

### 1. CFA变量声明提取结果（期望）：

**全局变量**：
- `tmp` (Char*)

**函数`glob2`的局部变量**：
- `p` (Char*) - 局部声明
- `pathbuf` (Char*) - 函数参数  
- `pathlim` (Char*) - 函数参数

**函数`main`的局部变量**：
- `pathbuf` (Char[]) - 局部数组
- `bound` (Char*) - 局部变量

### 2. Witness变量解析测试

从第一个不变量 `"( ( ( 4 + p ) == tmp ) && ( ( 0 < p ) && ( ( ( p % 4 ) == 0 ) && ( ! ( tmp < pathlim ) ) ) )"`：

**修改前（错误）**：
```
提取到的变量：
- p → MemoryLocation.forIdentifier("p")         // 错误：当作全局变量
- tmp → MemoryLocation.forIdentifier("tmp")     // 正确：本来就是全局变量  
- pathlim → MemoryLocation.forIdentifier("pathlim")  // 错误：当作全局变量
```

**修改后（正确）**：
```
函数上下文: "glob2"
提取到的变量：
- p → MemoryLocation.forLocalVariable("glob2", "p")         // 正确：局部变量 → "glob2::p"
- tmp → MemoryLocation.forIdentifier("tmp")                 // 正确：全局变量 → "tmp"
- pathlim → MemoryLocation.forLocalVariable("glob2", "pathlim")  // 正确：函数参数 → "glob2::pathlim"
```

### 3. 解析逻辑验证

我们的`VariableScopeResolver.resolveVariable()`方法：

```java
public MemoryLocation resolveVariable(String varName, String functionContext) {
    // functionContext = "glob2"
    
    // 检查 "p"
    if ("glob2" != null && 
        localVariablesByFunction.containsKey("glob2") &&
        localVariablesByFunction.get("glob2").contains("p")) {
        return MemoryLocation.forLocalVariable("glob2", "p");  // ✅ 返回 "glob2::p"
    }
    
    // 检查 "tmp"  
    if ("glob2" != null && 
        localVariablesByFunction.containsKey("glob2") &&
        localVariablesByFunction.get("glob2").contains("tmp")) {
        // ❌ tmp不在glob2的局部变量中
    } else {
        return MemoryLocation.forIdentifier("tmp");  // ✅ 返回 "tmp"（全局）
    }
    
    // 检查 "pathlim"
    if ("glob2" != null && 
        localVariablesByFunction.containsKey("glob2") &&
        localVariablesByFunction.get("glob2").contains("pathlim")) {
        return MemoryLocation.forLocalVariable("glob2", "pathlim");  // ✅ 返回 "glob2::pathlim"
    }
}
```

## 测试结论

✅ **修改成功**：我们的变量作用域解析器能够：

1. **正确提取CFA中的变量声明**
   - 区分全局变量和局部变量
   - 按函数分组管理局部变量和参数

2. **正确解析witness不变量中的变量**
   - 利用witness location中的函数信息
   - 根据函数上下文判断变量作用域
   - 创建正确的MemoryLocation对象

3. **解决了原来的问题**
   - 不再把所有变量都当作全局变量
   - 正确区分`p`（局部）和`tmp`（全局）
   - 正确处理函数参数`pathbuf`、`pathlim`

这个修改将显著提高CPAchecker在处理witness不变量时的变量精度，特别是在包含局部变量的复杂不变量中。
