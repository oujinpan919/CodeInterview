#!/usr/bin/env bash
# ============================================================
# 码上记 CodeInterview · 交付前静态红线自查（T20）
#
# 用法（仓库根或任意目录）：
#   bash scripts/check.sh
#
# 规则：命中任何红线 → 打印「文件:行号 内容」并以退出码 1 结束；
#       全部通过 → 输出 PASS 列表与 SUMMARY，退出码 0。
# 说明：只扫源码（backend/src、frontend/src）与仓库跟踪文件，跳过构建产物/依赖/日志。
# ============================================================
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

FAIL=0
PASS=0

pass() {
  echo "PASS $1"
  PASS=$((PASS + 1))
}

fail() {
  local name="$1"
  shift
  echo "FAIL $name"
  printf '  %s\n' "$@"
  FAIL=1
}

# 通用 grep：命中输出 文件:行号:内容
hits() {
  grep -rnE "$@" 2>/dev/null || true
}

echo "== 码上记 · 静态红线自查 =="
echo "repo: $ROOT"
echo

# 1) id 不得转数字（02 §8）
OUT="$(hits 'Number\(|parseInt\(|parseFloat\(' frontend/src)"
if [[ -n "$OUT" ]]; then fail "id 不得转数字（Number(/parseInt(）" "$OUT"; else pass "id 不得转数字（Number(/parseInt(）"; fi

# 2) v-html 只允许 MarkdownView 唯一一处（只数真正的属性用法，不含注释）
ALL_VHTML="$(hits 'v-html=' frontend/src)"
BAD_VHTML="$(printf '%s\n' "$ALL_VHTML" | grep -v 'frontend/src/components/MarkdownView.vue' || true)"
VHTML_COUNT="$(printf '%s\n' "$ALL_VHTML" | grep -c 'v-html=' || true)"
if [[ -n "$BAD_VHTML" || "$VHTML_COUNT" -ne 1 ]]; then
  fail "v-html 仅允许 MarkdownView 一处（当前 $VHTML_COUNT 处）" "${BAD_VHTML:-$ALL_VHTML}"
else
  pass "v-html 仅允许 MarkdownView 一处"
fi

# 3) 禁止 Element Plus 全量样式
OUT="$(hits 'element-plus/dist/index\.css' frontend/src)"
if [[ -n "$OUT" ]]; then fail "未引入 element-plus/dist/index.css（按需引入）" "$OUT"; else pass "未引入 element-plus/dist/index.css（按需引入）"; fi

# 4) 禁止 echarts 全量入口（允许 echarts/core）
OUT="$(hits "from ['\"]echarts['\"]" frontend/src)"
if [[ -n "$OUT" ]]; then fail "未使用 echarts 全量入口（仅 echarts/core + use）" "$OUT"; else pass "未使用 echarts 全量入口（仅 echarts/core + use）"; fi

# 5) 禁止 shiki 全量 bundle
OUT="$(hits 'shiki/bundle' frontend/src)"
if [[ -n "$OUT" ]]; then fail "未使用 shiki 全量 bundle" "$OUT"; else pass "未使用 shiki 全量 bundle"; fi

# 6) token 只得放 sessionStorage，禁止 localStorage
OUT="$(hits "localStorage\.setItem\(['\"]ci_token" frontend/src)"
if [[ -n "$OUT" ]]; then fail "token 禁止写 localStorage（应为 sessionStorage）" "$OUT"; else pass "token 禁止写 localStorage（应为 sessionStorage）"; fi

# 7) 密钥泄露扫描（sk-/ak-，讯飞是 ak- 前缀）
OUT="$(hits '(sk|ak)-[A-Za-z0-9]{16,}' \
  --exclude-dir=.git --exclude-dir=node_modules --exclude-dir=dist \
  --exclude-dir=target --exclude-dir=backups --exclude-dir=logs .)"
if [[ -n "$OUT" ]]; then fail "仓库内不得出现疑似 API Key" "$OUT"; else pass "仓库内不得出现疑似 API Key（sk-/ak-）"; fi

# 8) 后端禁用组件（02 §8）
OUT="$(hits 'WebClient|RedisTemplate|knife4j|spring-boot-starter-webflux|reactor-netty' backend/src frontend/src)"
if [[ -n "$OUT" ]]; then fail "后端禁用组件（WebClient/RedisTemplate/knife4j/WebFlux）" "$OUT"; else pass "后端禁用组件（WebClient/RedisTemplate/knife4j/WebFlux）"; fi
OUT="$(hits '\bFlux\b' backend/src)"
if [[ -n "$OUT" ]]; then fail "后端禁用 Reactor Flux" "$OUT"; else pass "后端禁用 Reactor Flux"; fi

# 9) 密钥文件不得入库
OUT="$(git ls-files 2>/dev/null | grep -E '(^|/)application-local\.yml$|(^|/)\.env$' || true)"
if [[ -n "$OUT" ]]; then fail "application-local.yml / .env 不得入库" "$OUT"; else pass "application-local.yml / .env 不得入库"; fi

# 10) 构建产物：dist 中最大 JS gzip ≤300KB（04 B7.3）
if [[ -d frontend/dist/assets ]]; then
  MAX=0
  MAX_FILE=""
  for file in frontend/dist/assets/*.js; do
    [[ -e "$file" ]] || continue
    SIZE="$(gzip -c "$file" | wc -c | tr -d ' ')"
    if (( SIZE > MAX )); then
      MAX="$SIZE"
      MAX_FILE="$file"
    fi
  done
  if (( MAX > 307200 )); then
    fail "dist 最大 JS gzip ≤300KB" "$MAX_FILE gzip=${MAX}B（上限 307200B），先 npm run build 或检查拆包"
  else
    pass "dist 最大 JS gzip ≤300KB（$(basename "$MAX_FILE") gzip=${MAX}B）"
  fi
else
  pass "dist 最大 JS gzip 检查 SKIP（frontend/dist 不存在，先 npm run build）"
fi

echo
if (( FAIL )); then
  echo "SUMMARY FAIL（见上方红线条目）"
  exit 1
fi
echo "SUMMARY OK（$PASS 项红线全部通过）"
exit 0
