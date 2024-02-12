# 化学推断器

![](https://img.shields.io/badge/Latest_Release-1.0-blue) ![](https://img.shields.io/badge/Development_Usage-JDK_17-orange)

该项目没有使用构建工具。

---

### 在使用之前你需要了解……

**限定器** 用于限定一个节点可能的物质，如“单质”或“氧化物”。如果向一个节点添加了两个限定器，该节点可能的物质将是这两个限定器所对应物质的交集。

**条件** 用于表示一个反应的条件。在 `reactions.txt` 中，不添加条件的反应会默认是“无反应条件”，表示为 `(Null)`；在输入的推断题文件中，不添加条件的节点关系会默认是“任意反应条件”，表示为 `(Any)`。

**强制条件** 用符号 `!` 表示，标记了该符号的节点关系在判断时将会要求反应需要完全符合，如下方反应

```
`α > β `heat` `MnO2`
```
满足节点关系
```
add A > B `heat`
```
但不满足节点关系 
```
add A > B ! `heat`
```
因此，若要表示一个节点关系不需要条件，可以使用
```
! `(Null)`
```

**类别** 用于限定一个反应的类别，如“放热反应”或“置换反应”。一个节点关系拥有多个类别时取交集（类似限定器）。

### 编写推断题文件

```
// 声明节点
node N...
// 设置节点 N 的物质已知，为 SUBSTANCE
set N substance SUBSTANCE
// 设置节点 N 已知的所有可能的物质
set N possibles SUBSTANCE...
// 设置节点 N 已知的所有可能的离子，取并集；离子格式如 {(H)+}
set N possible-ions ION...
// 设置节点 N 已知的所有限定器，取交集
set N limiters LIMITER...
// 添加节点反应
// 如 add N1 N2 表示 N1 与 N2 反应；
// 如 add > N1 N2 表示 N1 与 N2 共同被生成；
// 如 add N1 N2 > N3 N4 表示 N1 与 N2 共同生成 N3 与 N4；
add N...
add > N...
add N1... > N2... ! `CONDITION`... @CATEGORY...
// 一个例子：add A > B ! `ignite` @combination @exothermic
// 表示节点 A 生成节点 B，条件只有点燃；该反应属于化合反应、放热反应。
```

注意：推断题文件中的每一个 Token **必须用空格隔开**，每一条语句占用一行。例如
```
add A > B !`heat`
```
是错误的；
```
add A > B ! `heat`
```
是正确的。

---

### 编写 reactions.txt

`reactions.txt` 储存着程序可以用于推断的所有反应，要编写该文件，可以参考该文件的已有内容。

```
SUBSTANCE1... > SUBSTANCE2... `CONDITION`... @CATEGORY...
```

例如：

```
KClO3 > KCl O2 `catalyzer` `MnO2` @decomposition
```

表示 `KClO3` 可以生成 `KCl` 和 `O2`，反应条件为 `MnO2` 催化剂，属于分解反应。

如果要涉及到离子反应，只能出现两种情况：一是两种离子反应生成几种物质；二是置换反应。其他的反应，即使是离子反应，也需要将物质明确写出（因为加载器暂不支持）。

### 编写 ions.txt

`ions.txt` 储存着离子所对应的物质，要编写该文件，可以参考该文件的已有内容。

```
ION ~ SUBSTANCES...
```

例如：

```
{(H)+} ~ HCl H2SO4 HNO3
```

表示氢离子对应盐酸、硫酸和硝酸。

### 编写 limiters.txt

`limiters.txt` 储存着限定器所对应的物质，要编写该文件，可以参考该文件的已有内容。

```
LIMITER ~ SUBSTANCES...
```

例如：

```
[simple-substance] ~ Mg Al Zn Fe H2 Cu Ag C P S O2
```

表示单质限定器对应了镁、铝、锌、铁、氢气、铜、银、碳、磷、硫、氧气。

---

作者：[CarbonicX](https://space.bilibili.com/21635425)（bilibili）

该项目使用 GPL 许可证。

