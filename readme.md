概述：  
在网络结构上，印链采用P2P网络结构，使用TCP连接自定义消息结构通讯。

节点发现
---

节点发现一般有三种：  
1.源码中写死几个种子节点服务器地址，通过种子节点获取更多节点地址  
2.通过DNS获取种子节点服务器地址，通过种子节点获取更多节点地址  
3.通过HTTP请求获取子节点服务器地址，通过种子节点获取更多节点地址  

Inchain目前主要采用第二种节点发现方式。

共用结构
---

绝大多数整数都都使用little endian编码，只有IP地址或端口号使用big endian编码。

1.PeerAddress
> 印链封装的节点信息结构,兼容IPv6与IPv4地址。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|8| Timestamp |int64|当前时间|
|4|Service Version|uint32|服务版本|
|16|Peer Address|byte[16]|IPv6地址|
|2|Port|uint16|端口|

> Peer Address同时兼容IPv6与IPv4地址。  
> 当地址为IPv4时，地址存储在buf[12]开始的最后4字节处。同时设置buf[10]和buf[11]为0xFF。

2.InventoryItem

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|1|type|byte|清单类型|
|32|hash|byte[32]|哈希|

> 已定义的type值
> 
> |值|类型|说明|
> |---|---|---|
> |0x01|Transaction|交易|
> |0x02|Block|区块|
> |0x03|NewBlock|新区块|
> |0x04|Consensus|共识|

3.Transaction

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|type|int32|类型|
|4|version|uint32|版本|
|4|inputs size|int32|输入交易数量|
|?\*?|inputs|tx_in[]|输入交易表|
|4|inputs size|int32|输出交易数量|
|?\*?|outputs|tx_out[]|输出交易表|
|8| Timestamp |int64|交易时间戳|
|8|lockTime|int64|锁定时间|
|4|remark length|int32|备注长度|
|?|remark|uint8[?]|备注|

> lockTime 小于0永久锁定，大于等于0为锁定的时间或者区块高度  
> remark length为0时，说明remark为空  
> remark 为utf-8编码字符串

协议
---

### 公共结构
1.包头
> 所有对等节点通讯消息的包头。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|MagicNo|uint32|魔法值|
|12|Command|char[12]|命令类型|
|4|Payload Length|uint32|消息长度|
|4|Checksum|uint32|校验和|
|?|Payload|uint8[?]|消息体|

已定义的MagicNo值

> |值|说明|
> |---|---|
> |0x05209A2A|正式网|
> |0x2581D888|测试网|
> |0x0001B6A0|单元测试网|

Command采用utf8编码，长度为12字节，多余部分用0填充。

Checksum是Payload两次SHA256散列后的前4个字节。

Payload根据不同的命令有不同的详细格式，见下文 【消息结构】。


### 消息结构
---
version

> TCP连接成功后，Client主动发送给Remote Peer。  
> 在双向握手成功之前，不会有其他消息发送。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|1|localServices|uint8|哪个网络服务|
|4|client version|uint32|客户端版本|
|8|local time|uint64|时间戳|
|30|local peer address|PeerAddress|本机地址|
|30|remote peer address|PeerAddress|请求者地址|
|4|UserAgent length|uint32|版本字符串长度|
|?|UserAgent|char[?]|版本字符串|
|4|best block height|uint32|最适合高度|
|32|best block hash|char[32]|最适合高度块Hash字符串|
|8|nonce|int64|随机数|

---
verack
> 收到Version消息后，抽取nonce，响应version命令。


|尺寸|字段|数据类型|说明|
|---|---|---|---|
|8|current time|int64|当前时间戳|
|8|nonce|int64|收到的VersionMessage中的nonce值|

---
ping
> 心跳协议，探测对等节点是否正常提供服务。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|8|nonce|int64|随机数|

---
pong
> 心跳协议，响应ping命令。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|8|nonce|int64|收到的Ping包中的nonce|

---
getaddr
> 从对等节点请求握手成功的服务节点地址。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|8|time|int64|当前时间戳|

---
addr
> 响应getaddr命令，返回本地握手成功的节点信息。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|size|int32|节点数|
|30*?|peer address[]|PeerAddress[size]|服务节点信息|


---
inv
> 数据向量清单。最大Item长度不超过10000。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|size|int32| InventoryItem数量|
|33*?|invItem| InventoryItem[]|数据表|

---
getdatas
> 向对等节点发送下载数据的消息，包括区块和交易，一般用于回应inv命令。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|size|int32| InventoryItem数量|
|33*?|invItem| InventoryItem[]|数据表|

---
getblock

> 当节点启动时，发现本地区块比连接的对等节点区块高度小，则发送该消息下载同步区块信息。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|32|start hash|sha256|开始区块hash|
|32|stop hash|sha256|结束区块hash|

---
block

> 当对等节点收到getblock消息后，有能分享的数据，则使用block消息响应请求。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|version|uint32|版本|
|32|merkleHash|sha256|merkle根节点hash|
|4|Timespan|uint32|时间戳|
|4|height|uint32|高度|
|4|period count|uint32|该时段共识人数|
|4|period time|uint32|时段，一轮共识中的第几个时间段，可验证对应的共识人|
|4|period start time|uint32|本轮开始的时间点，单位（秒）|
|4|script length|uint32|签名脚本长度|
|?|script|char[?]|签名脚本，包含共识打包人信息和签名|
|?*?|Transactions|tx[]|交易列表|

---
notfound
> 当对等节点收到getblock消息后，没有能分享的数据，则回应notfound。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|32|hash|sha256|getblock命令请求的交易或区块hash|

TX协议
---





