package io.github.zzzyyylllty.kangeldungeon.event

import taboolib.platform.type.BukkitProxyEvent

class kangeldungeonReloadEvent() : BukkitProxyEvent()

/**
 * Modify [defaultData] you can register
 * your custom utils.
 * DO NOT USE clear, re-set or directly modify it, OR OTHER SENSITIVE FUNCTIONS.
 * */
class KAngelDungeonCustomScriptDataLoadEvent(
    var defaultData: LinkedHashMap<String, Any?>
) : BukkitProxyEvent()
