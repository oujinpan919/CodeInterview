/**
 * 目录提取器离线自测（node 直接跑）。
 * 用法：node scripts/test-outline.mjs
 *
 * 断言取自真实题库里几种典型答案结构：
 *  - 456（Optional）：行内强调 `**结论**：` 与 `**String** 不可变` 混排，必须只取到小节；
 *  - 453（接口默认方法）：带括号注 `**冲突解决三条规则**（按优先级）：`；
 *  - 列表项内的加粗 `1. **类优先**：` 不能算小节。
 */
import { extractOutline } from '../frontend/src/composables/useQuestionOutline.js'

const CASES = [
  {
    name: '456 · Optional（行内强调不得误判为小节）',
    markdown: `**结论**：\`Optional\` 的本意是在返回类型上显式表达「可能没有值」。

**orElse 与 orElseGet**：\`orElse(defaultValue)\` 的实参总会求值。

**反模式清单**：
1. **做字段类型**：\`Optional\` 不可序列化；
2. **做方法参数**：参数可空应重载方法；

**推荐写法**：链式 \`map\` 取值。

**String** 不可变，每次拼接产生新对象，这是行内强调不是小节。`,
    expect: ['结论', 'orElse 与 orElseGet', '反模式清单', '推荐写法']
  },
  {
    name: '453 · 带括号注的小节',
    markdown: `**结论**：\`default\` 方法让接口演进。

**冲突解决三条规则**（按优先级）：
1. **类优先**：父类中的具体方法胜过接口默认方法；

**static 与 private**：接口的 static 方法属于接口本身。

**不能有实例字段**：接口描述行为契约。`,
    expect: ['结论', '冲突解决三条规则', 'static 与 private', '不能有实例字段']
  },
  {
    name: '101 · 纯行内强调（无小节，应返回空）',
    markdown: `**String** 不可变，每次拼接产生新对象。

**StringBuilder** 可变、非线程安全，单线程拼接首选。

**StringBuffer** 可变、方法加 \`synchronized\`。`,
    expect: []
  },
  {
    name: '空内容',
    markdown: '',
    expect: []
  }
]

let failed = 0

for (const testCase of CASES) {
  const actual = extractOutline(testCase.markdown).map((item) => item.text)
  const pass = JSON.stringify(actual) === JSON.stringify(testCase.expect)
  if (!pass) failed += 1
  console.log(`${pass ? 'PASS' : 'FAIL'}  ${testCase.name}`)
  if (!pass) {
    console.log(`  expect: ${JSON.stringify(testCase.expect)}`)
    console.log(`  actual: ${JSON.stringify(actual)}`)
  }
}

if (failed > 0) {
  console.error(`\n${failed} case(s) failed`)
  process.exit(1)
}
console.log('\nall cases passed')
