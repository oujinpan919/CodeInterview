# ============================================================
# 码上记 CodeInterview · 测试数据库准备脚本
# ------------------------------------------------------------
# 作用：
#   1) 创建/重建 codeinterview_test（只允许以 _test 结尾的库名）
#   2) 把当前开发库的结构 + 数据复制进去，保证测试与现有 seed 一致
#   3) 创建仅供本机测试使用的最小权限账号 codeinterview_test
#
# 为什么需要：
#   ChatTestSupport 会 DELETE 多张 AI 表以隔离用例；若测试直接连开发库，
#   会把真实的 AI 会话/凭证清空。测试必须连独立库。
#
# 用法（PowerShell 5+）：
#   powershell -ExecutionPolicy Bypass -File scripts/setup-test-db.ps1 `
#     -MySqlBin "D:\Develop\MySQL\MySQLServer8.0\bin"
#
# 然后：
#   cd backend
#   mvn.cmd -q test
# ============================================================

param(
    [string]$MySqlBin = "D:\Develop\MySQL\MySQLServer8.0\bin",
    [string]$SourceDatabase = "codeinterview",
    [string]$TestDatabase = "codeinterview_test",
    [string]$RootUser = "root",
    [string]$TestUser = "codeinterview_test",
    [string]$TestPassword = "codeinterview_test"
)

$ErrorActionPreference = "Stop"

if (-not $TestDatabase.EndsWith("_test")) {
    throw "TestDatabase 必须以 _test 结尾，拒绝操作：$TestDatabase"
}
if ($TestDatabase -eq $SourceDatabase) {
    throw "测试库不能与源库相同"
}

$mysql = Join-Path $MySqlBin "mysql.exe"
$mysqldump = Join-Path $MySqlBin "mysqldump.exe"
if (-not (Test-Path $mysql) -or -not (Test-Path $mysqldump)) {
    throw "找不到 mysql.exe / mysqldump.exe，请用 -MySqlBin 指定 MySQL bin 目录"
}

$secureRootPwd = Read-Host "输入 MySQL root 密码" -AsSecureString
$rootPwd = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureRootPwd))
$env:MYSQL_PWD = $rootPwd

$dumpFile = Join-Path $env:TEMP ("codeinterview_test_" + [Guid]::NewGuid().ToString("N") + ".sql")
try {
    Write-Host "1/4 导出源库结构 + 数据：$SourceDatabase"
    & $mysqldump -u $RootUser --default-character-set=utf8mb4 --single-transaction `
        --routines --triggers --result-file=$dumpFile $SourceDatabase
    if ($LASTEXITCODE -ne 0) { throw "mysqldump 失败" }

    Write-Host "2/4 重建测试库：$TestDatabase"
    & $mysql -u $RootUser -e "DROP DATABASE IF EXISTS ``$TestDatabase``; CREATE DATABASE ``$TestDatabase`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
    if ($LASTEXITCODE -ne 0) { throw "创建测试库失败" }

    Write-Host "3/4 导入测试库"
    $sourcePath = $dumpFile.Replace('\', '/')
    & $mysql -u $RootUser $TestDatabase -e "source $sourcePath"
    if ($LASTEXITCODE -ne 0) { throw "导入测试库失败" }

    Write-Host "4/4 创建测试账号并授权（仅 codeinterview_test.* 的 DML）"
    $grantSql = @"
CREATE USER IF NOT EXISTS '$TestUser'@'localhost' IDENTIFIED BY '$TestPassword';
ALTER USER '$TestUser'@'localhost' IDENTIFIED BY '$TestPassword';
CREATE USER IF NOT EXISTS '$TestUser'@'%' IDENTIFIED BY '$TestPassword';
ALTER USER '$TestUser'@'%' IDENTIFIED BY '$TestPassword';
GRANT SELECT, INSERT, UPDATE, DELETE ON ``$TestDatabase``.* TO '$TestUser'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON ``$TestDatabase``.* TO '$TestUser'@'%';
FLUSH PRIVILEGES;
"@
    & $mysql -u $RootUser -e $grantSql
    if ($LASTEXITCODE -ne 0) { throw "创建测试账号/授权失败" }

    Write-Host "完成：$TestDatabase 已就绪（测试账号 $TestUser）"
}
finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    if (Test-Path $dumpFile) { Remove-Item $dumpFile -Force -ErrorAction SilentlyContinue }
}
