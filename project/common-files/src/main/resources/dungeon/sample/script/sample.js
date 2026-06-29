// ========================================
// 示例 JS 脚本 - 可直接在 instance.runScript("sample") 中调用
// 文件名（不含扩展名）即为脚本名称
// 脚本中可直接使用所有 defaultData 注册的全局变量：
//   instance, template, player, Bukkit, Sys, Math, mmUtil, ...
// ========================================

// 发送消息到控制台
// Sys.println("Sample JS script executed!");

// 向地牢中所有玩家发送消息
// instance.sendMessageToAllPlayers("<yellow>JS 脚本执行成功！</yellow>");

// 获取并设置地牢元数据
// var count = instance.getMetaAsInt("script_executions") || 0;
// instance.setMeta("script_executions", count + 1);

// 检测变量注入
// if (typeof scriptName !== 'undefined') {
//     instance.sendMessageToAllPlayers("<green>脚本名称: " + scriptName + "</green>");
// }

// 使用 Bukkit API
// var world = Bukkit.getWorld(instance.worldName);
// if (world) {
//     Sys.println("当前地牢世界: " + world.getName());
// }
