# webchat

#### 介绍
基于SpringBoot+Vue3+WebSocket在线聊天室

#### 项目介绍
仿WX+QQ界面与功能，内部包含三个角色（用户，管理员，所有者），管理员可禁言、限制登录用户，所有者可设置管理员、头衔等功能

内部包含数据安全传输框架，基于SHA，AES，RSA加密交换数据

#### 现支持的功能

#### 登录

* 账号/UID登录
* 微信登陆（需要配置公众号，通过公众号认证微信用户。特殊的：通过微信内置浏览器可直接登录，其他浏览器仅显示登录二维码）
* 异地登录检查（通过IP地址检查不同省份，需要验证邮箱后才能登录。没有绑定邮箱的账号没有此验证[通过微信首次登录不绑定邮箱]）
* 通过邮箱注册
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
* 永久限制登录用户（通过UID，所有者权限）
* 发布公告

#### 消息

* 支持文字，emoji，图片，语音，视频[<=30M]，文件[<=30M]消息
* 聊天记录离线保存
* 未读消息显示数量（最大99）
* 已读指定用户的所有消息（包括语音消息）
* 未读语音消息红点提示
* 撤回消息（2分钟以内）
* 移除消息（仅本地）
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
* 全屏
* 读取粘贴板图片（复制图片后，在消息输入框粘贴，需要粘贴板权限）
* 文件拖拽支持（可将文件直接拖拽到消息列表发送，通过扩展名识别文件类型）

#### 软件架构

基于JDK11的聊天室系统
* 后端：SpringBooot、Mybatis Plus、Shiro、Mysql、Redis、Kafka、Socket、FTP
* 前端：HTML5、css3、javaScript(ES6+)、Vue3(原生)、Element Plus(原生)、WebSocket、WebRTC

需要配置的环境依赖
* MySQL 5.7
* Redis 6.2
* Kafka 2.9
* FTP服务器

- by 浮生如梦