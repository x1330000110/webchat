# webchat

#### 项目介绍

由SpringBoot+WebSocket构建，完整实现了HTML5网页在线多人聊天功能。支持各种消息发送和查看，聊天记录离线保存，未读消息记录，视频语音通话（WebRTC），个性化设置以及管理员权限。

WebScoket消息发送，重要API接口已被加密，有关HTTP数据加密/验证支持欢迎一起交流 [参考文档](https://www.zybuluo.com/1330000110/note/2172127)。

#### 层级结构

```
                                                                                                | -----> GroupCommandParser
                                                                                                |
--------------                             |----> PermissionManager ------> CommandParser ------| -----> UserCommandParser
webchat-client -------> SocketEndpoint ----|                                                    |
--------------                             |----> SettingSupport                                | -----> PermissCommandParser

       ⇑
 [webchat-core]        |---> message
       ⇓               |                      | ------> BaiduSpeech
                       |---> resource --------| 
--------------         |                      | ------> ResourceStorage
webchat-server --------|---> group
--------------         |                      |---> admin
                       |---> user ------------|
                       |                      |---> owner
                       |                                       | ------> WX
                       |---> login ----------> shiro ----------|
                                                               | ------> QQ

                                        | -----> RSA
                       | ---> core -----|
--------------         |                | -----> AES
webchat-secure --------|                                        | ----> ExpiredValidator
--------------         |                                        |                               | -----> MappedRepeatValidator
                       | ---> filter --------> validator -------| ----> RepeatValidator --------|
                                                                |                               | -----> RedisRepeatValidator
                                                                | ----> SignatureValidator
```

基于JDK1.8

* 后端：Spring Boot、Mybatis Plus、Shiro、MySQL、Redis、Kafka、WebSocket、Nacos、Openfeign

必要的依赖信息

* MySQL 5.7.1
* Redis 7.0.4
* Kafka 2.3.2
* Nacos 2.2.0
* 系统邮箱账号
* 微信公众号
* FTP/Lanzou（或自己重写资源储存方式）

#### 现支持的功能

#### 登录

* 邮箱/UID登录
* 微信登陆
* QQ登录
* 异地登录检查
* 通过邮箱注册
* 找回密码

#### 用户

* 添加好友（未来）
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

##### 用户在线状态监控说明（头像右下角标识）

* 绿色：用户在线 <br>
  手机端：用户正在浏览当前页面 <br>
  电脑端：鼠标在页面中 <br>
* 黄色：繁忙 <br>
  手机端：浏览其他网页，浏览器切到后台，锁屏3秒均会触发 <br>
  电脑端：鼠标离开页面1分钟触发 <br>
* 红色：离开 <br>
  手机端：当前页面无任何触摸操作 (因部分安卓系统后台限制，网页切到后台/锁屏后计时器将被阻塞) <br>
  电脑端：鼠标在页面未移动或移出页面10分钟后触发 <br>
* 灰色头像无标识：离线 <br>
  发送的消息将被离线保存 <br>

#### 群组

* 创建，加入，退出，解散群组
* 设置入群密码
* 群内踢人（未来）
* 修改群资料（未来）
* 入群审核（未来）
* 转让群（未来）

#### 消息

* 支持文字，Emoji，图片，语音，视频，文件消息
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
* 视频预览
* 音乐文件播放
* 清空所有消息

#### 媒体（需要HTTPS协议）

* 录制视频
* 视频通话
* 语音通话

#### 其他

* 修改背景图片
* 修改气泡颜色
* 切换主题
* 桌面消息通知（需要浏览器支持）
* 消息声音
* 消息震动（仅手机）
* 来电铃声
* 全屏
* 读取粘贴板图片
* 文件拖拽支持
* 所有者接入小冰机器人
* 敏感关键词屏蔽
* 全员禁言
