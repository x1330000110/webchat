# webchat

#### 项目介绍

基于SpringBoot+WebSocket的实时通信系统。支持多人在线聊天，在线状态实时更新，支持多种类型消息发送和查看，支持用户黑名单，禁言，限制登录，视频录制，视频语音通话。更有个性化设置，背景图片自定义，黑暗主题，消息通知，自定义来电铃声，全员禁言，AI消息自动回复等功能。

内部包含数据安全传输模块，基于AES，RSA加密交换数据（可独立提取使用：[参考文档](https://www.zybuluo.com/1330000110/note/2172127)）。

#### 层级结构
```
                                                                         | ---> UserManager -------> UserChangeLinstener
                                                                         |
--------------                             |----> PermissionManager -----| ---> GroupManager ------> GroupChangeLinstener
webchat-client -------> SocketEndpoint ----|                             |
--------------                             |----> SettingSupport         | ---> RedisManager

                       |---> user
                       |
                       |---> message
------------           |                      | ------> BaiduSpeech
webchat-core ----------|---> resource --------| 
------------           |                      | ------> 云储存
                       |---> owner
                       |                      | ------> wechat
                       |---> login -----------|
                                              | ------> shiro

--------------         | ---> core
webchat-secure --------|                                        | ----> MappedRepeatValidator
--------------         | ---> filter --------> validator -------|
                                                                | ----> RedisRepeatValidator
```

基于JDK11

* 后端：Spring Boot、Mybatis Plus、Shiro、MySQL、Redis、Kafka、Socket

需要配置的环境依赖

* MySQL 5.7
* Redis 6.2
* Kafka 3.1

#### 现支持的功能

#### 登录

* 邮箱/UID登录
* 微信登陆
* 异地登录检查
* 通过邮箱注册（填充qq邮箱昵称头像）
* 找回密码
* 识别登录平台（PC，安卓，iphone）

#### 用户

* 查看资料
* 修改个人资料（包括头像）
* 修改密码（未绑定邮箱无法修改密码）
* 修改邮箱
* 私聊（切换私聊的用户，将已读非语音的所有消息）
* 屏蔽（包括通话功能）
* 禁言（1秒-7天，管理员权限）
* 限制登录（1秒-30天，管理员权限）
* 设置头衔（所有者权限）
* 设置管理员（所有者权限）
* 移除消息（通过消息ID，所有者权限）
* 永久限制登录（通过UID，所有者权限）
* 发布公告（所有者权限）


### 用户在线状态监控说明（头像右下角标识）

* 绿色：用户在线 
    手机端：用户正在浏览当前页面
    电脑端：鼠标在页面中
* 黄色：繁忙
    手机端：浏览其他网页，浏览器切到后台，锁屏3秒均会触发
    电脑端：鼠标离开页面1分钟触发
* 红色：离开
    手机端：当前页面无任何触摸操作 (因部分安卓系统后台限制，网页切到后台/锁屏后计时器将被阻塞)
    电脑端：鼠标在页面未移动或移出页面10分钟后触发
* 灰色头像无标识：离线
    发送的消息将被离线保存

#### 消息

* 支持文字，emoji，图片，语音，视频，文件消息
* 聊天记录离线保存
* 未读消息显示数量（最大99）
* 已读指定用户的所有消息（包括语音消息）
* 未读语音消息红点提示
* 撤回消息（2分钟以内）
* 移除消息
* 设为背景（仅图片消息）
* 设为来电铃声（仅音乐类型文件）
* 语音转文字（来自百度识别API）
* 复制文字
* 保存文件
* 视频预览 (DPlayer)
* 音乐文件播放（mp3,wav）
* 清空所有消息

#### 媒体（需要HTTPS协议）

* 录制视频
* 视频通话
* 语音通话

#### 其他

* 修改背景图片
* 修改气泡颜色（本地）
* 切换主题
* 桌面消息通知（需要浏览器支持）
* 消息声音
* 消息震动（仅手机）
* 来电铃声
* 全屏
* 读取粘贴板图片（复制图片后，在消息输入框粘贴，需要粘贴板权限）
* 文件拖拽支持（可将文件直接拖拽到消息列表发送，通过扩展名识别文件类型）
* 所有者接入小冰机器人（仅在所有者离线时，发送文字消息将由小冰接管）
* 敏感关键词屏蔽
* 全员禁言
