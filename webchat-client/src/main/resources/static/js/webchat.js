const app = Vue.createApp({
    data() {
        return {
            // 手机标识
            ismobile: window.ismobile,

            // 个人信息
            myself: {
                shields: []
            },

            // 缓存
            cache: {
                // 选中的用户
                user: {},
                //  选中的消息
                message: {},
                // 正在播放的音频
                audio: {}
            },

            // 禁用状态
            disabled: {
                // 视频发起按钮
                call: false,
                // 通话离开按钮
                leave: false,
            },

            // popover
            popover: {
                // 消息工具
                message: false,
                // 转文字
                convert: false
            },

            // 最小化
            mini: {
                audio: false,
                video: false
            },

            // 模态框
            dialog: {
                // 密码
                password: false,
                // 资料
                material: false,
                // 粘贴板
                clipboard: false,
                // 设置
                setting: false,
                // 消息详情
                details: false,
                // 录制视频
                recorder: false,
                // 视频播放器
                player: false,
                // 内置登录
                login: false,
                // 内置注册
                register: false
            },

            // 加载中
            loads: {
                // 上传
                upload: false,
                // 设置中
                optional: false,
                // 加载聊天记录
                page: false,
                // 录音
                record: false,
                // 会话切换动画
                session: false,
                // 移除消息
                rm_msg: false,
                // 移除用户
                rm_user: false,
                // 语音转文字中
                convert: false,
                // 初始化媒体
                stream: false
            },

            // 粘贴板
            clipboard: {
                url: '',
                blob: null
            },

            // 用户右键菜单
            menu: {
                top: 0,
                left: 0,
                show: false,
                // 限制信息
                limit: {
                    time: 0,
                    max: 60,
                    unit: 60,
                    type: ''
                }
            },

            // webrtc
            webrtc: {
                startBtn: '',
                // 连接目标
                user: {},
                // 视频工具栏
                initTools: true,
                videoToolTimer: null,
                // webrtc对象
                client: null,
                // 前/后摄像头
                camera: true,
                // 连接时间
                connected: 0,
                connectionTime: '00:00',
                // 计时器
                timer: {
                    starter: null,
                    timeout: null,
                    connect: null
                },
                // 对话框
                dialog: {
                    audio: false,
                    video: false,
                    phone: false
                }
            },

            // 媒体
            media: {
                // 提示音
                audio: new MediaPlayer(),
                notice: new MediaPlayer(),
                phone: new MediaPlayer(true),
                // 录制工具
                recorder: {
                    audio: new AudioRecorder(60),
                    video: new VideoRecorder()
                },
                // 正在录制
                recording: false,
                recordTime: '00:00',
                recordTimer: null,
                // 录音数据
                audioBlob: null,
                // 录像数据
                videoBlob: null,
                // 图片预览
                viewer: new Viewer(),
                // 扩展视频播放器
                DPlayer: null,
            },

            // 输入框
            input: {
                content: '',
                placeholder: '',
                disabled: true,
                deftips: 'Enter message...',
                intervar: null,
                timer: null,
                // 用户@功能
                popper: false,
                filter: ''
            },

            // 聊天室数据
            room: {
                online: 0,
                messages: [],
                indexes: {},
                cache: [],
                user: {
                    messages: []
                },
                page: true,
                client: null
            },

            // 设置
            setting: {
                // 背景图片
                bgURL: null,
                // 主题
                theme: false,
                sub_theme: false,
                sub_rgb: '',
                // 私聊通知
                notify: false,
                tips: true,
                voice: true,
                vibrate: false,
                // 群组通知
                g_notify: false,
                g_tips: true,
                g_voice: false,
                g_vibrate: false,
                // 来电通知
                phone: true,
                p_voice: true,
                p_voice_src: '',
                p_vibrate: false,
                // 登录退出通知
                join: true,
                exit: false,
                // 当前选择的用户
                target: Ws.group,
                // 气泡颜色
                bubble: '#212121'
            },

            // 用户列表
            userList: [],

            // 固定群组信息
            groupData: {
                name: Ws.groupName,
                uid: Ws.group,
                online: true,
                lastTime: Wss.formatTime(),
                preview: null,
                messages: [],
                isgroup: true
            },

            // Ws统一标记
            Ws: {},

            // 兼容性测试工具
            Support: {},

            // 所有者操作框
            owner: {
                mid: '',
                uid: '',
                announce: ''
            },

            // 登录数据
            login: {
                auto: false,
                user: '',
                pass: '',
                code: '',
                wxuuid: '',
                // 定时器
                check: null,
                rules: {
                    user: {required: true, message: '请输入账号'},
                    pass: {required: true, message: '请输入密码'},
                    code: {required: true, message: '请输入验证码'}
                }
            },

            // 表单数据集
            formData: {
                name: '',
                user: '',
                email: '',
                pass: '',
                repeat: '',
                code: '',
                newcode: '',
                rules: {
                    name: [{required: true, message: '请输入昵称'}, {max: 8, message: '昵称格式不正确'}],
                    user: {required: true, message: '请输入UID/邮箱'},
                    email: [{required: true, message: '请输入邮箱'}, {type: 'email', message: '邮箱格式不正确'}],
                    pass: [{required: true, message: '请输入密码'}, {min: 8, max: 16, message: '密码格式不正确'}],
                    repeat: [
                        {required: true, message: '重复输入密码'},
                        {validator: (r, v, c) => v === this.formData.pass ? c() : c(new Error('两次密码不一致'))}
                    ],
                    code: {required: true, message: '请输入验证码'},
                    newcode: {required: true, message: '请输入验证码'}
                }
            },

            // emoji列表
            emojiMap: {},

            // 预设气泡颜色
            predefine: ['#394aab', '#523289', '#c45656', '#009688', '#212121'],

            // 滑动对象
            touch: null,

            // 消息滚动条
            scroll: null,

            // 发送事件优化
            inputevt: ismobile ? 'touchend' : 'click',

            // 通知对象
            globalTips: null,

            // 全局拖拽模态框
            dragBox: false,

            // 全屏状态
            fullscreen: false,

            // 设置页面菜单绑定对象
            activeTab: '1',
            collapse: '1'
        }
    },

    methods: {
        // 接受远程连接
        acceptRemote() {
            const target = this.webrtc.user.uid
            this.webrtc.type === Ws.video ? this.openVideoDialog(target) : this.openAudioDialog(target)
            this.webrtc.dialog.phone = false
        },

        // 添加文字表情
        insertEmoji(event) {
            const span = event.path.find(p => p.tagName === 'SPAN')
            if (!span) return
            // 获取当前输入框光标位置
            const index = this.$refs.input.input.selectionStart
            // 写入表情标记
            const content = this.input.content
            this.input.content = content.substr(0, index) + span.getAttribute('emoji') + content.substr(index)
        },

        // 全屏
        beFull() {
            this.fullscreen ? window.exitFull() : window.beFull()
            this.fullscreen = !this.fullscreen
        },

        // 用户消息快捷绑定
        bindmsg(wsmsg) {
            const user = this.finduser(wsmsg.uid)
            if (user == null) return null
            // 绑定快捷操作
            wsmsg.isblob = wsmsg.type === Ws.blob
            wsmsg.istext = wsmsg.type === Ws.text
            wsmsg.isimage = wsmsg.type === Ws.img
            wsmsg.isaudio = wsmsg.type === Ws.audio
            wsmsg.isvideo = wsmsg.type === Ws.video
            wsmsg.isgroup = wsmsg.target === Ws.group
            wsmsg.isself = wsmsg.uid === this.myself.uid
            wsmsg.istarget = [wsmsg.uid, wsmsg.target].includes(this.setting.target)
            wsmsg.savepath = wsmsg.isgroup || wsmsg.isself ? wsmsg.target : wsmsg.uid
            // 初始化消息
            wsmsg.user = user
            wsmsg.type = wsmsg.type || Ws.info
            wsmsg.position = wsmsg.isself ? 'cright' : 'cleft'
            wsmsg.preview = wsmsg.istext ? wsmsg.content : `[${this.parseType(wsmsg)}]`
            wsmsg.createTime = wsmsg.createTime ? new Date(wsmsg.createTime).getTime() : Date.now()
            typeof wsmsg.data === 'string' && (wsmsg.data = JSON.parse(wsmsg.data))
            // 语音消息
            if (wsmsg.isaudio) {
                // playing: 播放状态
                wsmsg.playing = false
                // 未读
                wsmsg.unread = !wsmsg.isself && (wsmsg.unread ?? true)
                // 转换的文本
                wsmsg.text = ''
            }
            // 过期判断
            wsmsg.isblob || wsmsg.isvideo && (wsmsg.expired = wsmsg.createTime + 3 * 24 * 60 * 60 * 1000 < Date.now())
            // 文件消息
            if (wsmsg.isblob) {
                // 音乐类型判断
                const idx1 = wsmsg.content.lastIndexOf('.')
                if (idx1 > -1) {
                    const ext = wsmsg.content.substr(idx1 + 1)
                    if (['mp3', 'wav', 'ogg'].includes(ext)) {
                        wsmsg.canPlay = true
                    }
                }
                // 预览数据
                const data = wsmsg.expired ? '已过期' : wsmsg.data.size
                const fname = wsmsg.content, idx2 = fname.lastIndexOf('.')
                const ext = idx2 > -1 ? fname.substr(idx2 + 1) : ''
                wsmsg.describe = ext.toUpperCase() + '文件 [' + data + ']'
            }
            return wsmsg
        },

        // 构建用户消息（生成唯一mid用于撤回）
        buildmsg(content, type, data, mid) {
            const uid = this.myself.uid, target = this.setting.target
            return {
                sysmsg: false,
                uid: uid,
                content: content,
                target: target,
                type: type || Ws.text,
                data: data,
                mid: mid || CryptoJS.MD5(uid + content + type + target + Date.now()).toString(),
            }
        },

        // 构建系统消息
        buildsys(command, ws, target, data) {
            return {
                sysmsg: true,
                target: target,
                type: ws || Ws.info,
                content: command,
                data: data
            }
        },

        // 检查指定消息是否可以撤回
        canWithdraw() {
            const wsmsg = this.cache.message
            return wsmsg.isself && !wsmsg.reject && Date.now() - wsmsg.createTime <= 0x1d4c0
        },

        // 变更气泡显示
        changeBubble(color, init) {
            this.setting.bubble = color
            init || this.showTips('切换成功')
        },

        // 设置限制时间单位
        changeTimeUnit() {
            const limit = this.menu.limit
            if (limit.unit === 60)
                limit.max = 60
            if (limit.unit === 3600)
                limit.max = 24
            if (limit.unit === 86400)
                limit.max = this.menu.limit.type === Ws.mute ? 7 : 30
            limit.time = 1
        },

        // 清空语音录制缓存
        clearAudio() {
            if (this.media.audioBlob == null)
                return
            this.input.disabled = false
            this.input.placeholder = this.input.deftips
            this.media.audioBlob = null
        },

        // 清空视频录制缓存
        clearVideo() {
            if (this.media.videoBlob == null)
                return
            this.media.videoBlob = null
            const video = $('#recorder-video')
            video.muted = true
            video.srcObject = this.media.recorder.video.stream
            video.play()
            window.navigator.vibrate([30])
        },

        // 清空消息
        clearMsg() {
            window.confirm('聊天记录一旦清空无法恢复，是否继续?', '注意', {
                confirmButtonText: '继续',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                axios.post('/message/clear', {
                    target: this.setting.target,
                    sign: CryptoJS.sign(this.setting.target)
                }).then(() => {
                    this.room.hasNext = false
                    this.room.user.messages = []
                    this.showTips('清除成功', Ws.success)
                })
            })
        },

        // 清除背景图片
        clearBackground() {
            document.body.style.removeProperty('--bg')
            this.setting.bgURL = ''
            this.showTips('清除成功')
        },

        // 关闭此页面所有popover
        closePopover() {
            this.popover.message = false
            this.popover.convert = false
            this.menu.show = false
            this.$refs.options?.hide()
            this.$refs.more?.hide()
            this.$refs.phone?.hide()
            this.$refs.emoji?.hide()
            this.disposeRecorder()
        },

        // 断开远程连接
        async closeRemote() {
            const connected = this.webrtc.connected
            const waiting = this.loads.call
            // 断开媒体连接
            this.webrtc.client?.dispose()
            // 停止等待提示音
            await this.media.phone.stop()
            // 重置数据
            this.webrtc.camera = true
            this.webrtc.connected = null
            !!(connected || waiting) && this.sendsys(this.webrtc.user.uid, Ws.leave)
            // 中断所有计时器
            Object.values(this.webrtc.timer).forEach(t => clearTimeout(t) && clearInterval(t))
            // 中断提示
            if (connected) {
                this.disabled.leave = true
                const template = `本次通话时长 ${this.webrtc.connectionTime}`
                this.pushsys(template, Ws.info, this.webrtc.user.uid)
                await this.media.notice.start('/wav/leave.wav')
                this.webrtc.connectionTime = '00:00'
            }
            // 关闭对话框
            this.webrtc.dialog.video = this.webrtc.dialog.audio = false
            this.mini.video = this.mini.audio = false
            this.loads.call = this.disabled.call = this.disabled.leave = false
        },

        // 复制消息
        copyText(text) {
            text = text || this.cache.message.content
            const textarea = document.createElement('textarea');
            document.body.appendChild(textarea);
            // 隐藏此输入框
            textarea.style.position = 'fixed';
            textarea.style.clip = 'rect(0 0 0 0)';
            textarea.value = text;
            textarea.select();
            // 复制
            document.execCommand('copy', true);
            // 移除输入框
            document.body.removeChild(textarea);
            this.showTips('已复制', Ws.success)
        },

        // 转换语音消息
        async convertAudio() {
            const wsmsg = this.cache.message
            await Wss.sleep(200)
            this.popover.convert = true
            // 检查缓存
            if (wsmsg.text) {
                return
            }
            // 提交服务器转换
            this.loads.convert = this.popover.convert = true
            axios.get('/resource/convert/' + wsmsg.mid).then(async response => {
                const data = response.data
                this.popover.convert = false
                if (data.success && data.data) {
                    await Wss.sleep(100)
                    wsmsg.text = data.data
                    this.popover.convert = true
                    return
                }
                data.data || this.showTips('未识别到消息内容', Ws.warning)
                data.success || this.showTips('转换失败', Ws.error)
            }).catch(() => {
                this.showTips('服务器繁忙，请稍后再试', Ws.error)
            }).finally(() => {
                this.loads.convert = false
            })
        },

        // 关闭录像机
        disposeRecorder() {
            this.media.recorder.video.dispose()
            $('#recorder-video')?.pause()
            clearInterval(this.media.recordTimer)
            this.media.recordTime = '00:00'
            this.media.videoBlob = null
            this.media.recording = false
            this.dialog.recorder = false
            this.webrtc.camera = true
        },

        // 保存文件
        saveBlob(wsmsg) {
            wsmsg = wsmsg || this.cache.message
            const download = (url, name) => {
                const link = document.createElement('a')
                link.href = url
                link.download = name
                link.click()
            }
            // 图片/视频保存手动设置文件名
            if (wsmsg.isvideo || wsmsg.isimage) {
                download('/resource/' + wsmsg.mid, Date.now() + (wsmsg.isvideo ? '.mp4' : '.jpeg'))
                return
            }
            // 文件保存
            download('/resource/' + wsmsg.mid, wsmsg.content)
        },

        // 通过uid获取用户信息
        finduser(uid) {
            if (uid === this.myself.uid) return this.myself
            return this.userList.find(user => user.uid === uid)
        },

        // 过滤用户
        filterUser(node, data) {
            const filter = this.input.filter
            return data.name.includes(filter) || data.uid.includes(filter)
        },

        // 获取当前消息图片预览集合
        getPreviewList(url) {
            const urls = this.room.user.messages.filter(m => m.isimage).map(m => m.content)
            // 根据当前url分割数组
            const index = urls.indexOf(url)
            const slice = urls.slice(index)
            slice.push(...urls.slice(0, index))
            return slice
        },

        // 获取用户状态
        getUserState(user) {
            if (!user.uid) return ''
            if (user.uid === Ws.group) return this.room.online + '人在线'
            const shield = this.myself.shields.includes(user.uid)
            return shield ? '已屏蔽' : user.online ? (user.platform || '在线') : '离线'
        },

        // 获取图片缩放高度
        getScaleHeight(data) {
            const {width, height, random} = data
            const scale = parseInt(getComputedStyle(document.documentElement).getPropertyValue('--image-scale-width'))
            return Math.round(scale / width * height) + 'px'
        },

        // 获取微信快速登录链接
        getWxFastURL() {
            const uuid = this.login.wxuuid = Math.random().toString(16).slice(2)
            // 微信浏览器直接登录
            const iswxmobile = navigator.userAgent.toLowerCase().match(/MicroMessenger/i)
            if (iswxmobile) {
                this.$refs.wxpopover.hide()
                axios.get('/api/wxfasturl/' + uuid + "$1").then(response => {
                    const json = response.data
                    json.success && (window.location.href = atob(json.data))
                })
                return
            }
            // TODO 虽然可以实现 但不是官方写法 待定
            this.$refs.wxcode._.props.src = '/api/wxqrcode/' + uuid
            // 启用定时器
            if (!this.login.check) {
                this.login.check = setInterval(async () => {
                    const response = await axios.post('/api/wxstate/' + uuid)
                    const data = response.data
                    if (data.code === 100) return
                    if (data.success) {
                        this.showTips(data.message, Ws.success)
                        this.dialog.login = false
                        this.initWebSocket()
                        this.$refs.wxpopover.hide()
                        return
                    }
                    this.clearWxInterval()
                    this.getWxFastURL()
                }, 1000)
            }
        },

        // 清空微信登录定时器
        clearWxInterval() {
            clearInterval(this.login.check)
            this.login.check = null
        },

        // 全局聊天框文件拖拽事件（部分浏览器不兼容）
        initDrogFile() {
            const dropbox = document.querySelector('.lite-chatbox')
            dropbox.ondrop = e => {
                e.preventDefault();
                this.dragBox = false
                const files = e.dataTransfer.files
                if (files.length > 1) {
                    this.showTips('仅支持拖拽上传单文件', Ws.warning)
                    return
                }
                if (!files[0].type) {
                    this.showTips('未能识别的文件类型', Ws.warning)
                    return
                }
                this.uploadBlob(files[0])
            }
            let timer;
            dropbox.ondragover = e => {
                e.preventDefault();
                if (timer) {
                    clearTimeout(timer)
                    timer = null
                }
                if (!this.dragBox) {
                    this.dragBox = true
                }
            }
            dropbox.ondragleave = e => {
                e.preventDefault();
                timer = setTimeout(() => this.dragBox = false, 200)
            }
        },

        // 初始化粘贴板事件
        initClipboard(input) {
            input.onpaste = async event => {
                try {
                    const data = await Clipboarder.read(event)
                    if (typeof data === 'string') return
                    event.preventDefault()
                    // 读取粘贴板
                    this.clipboard.blob = data
                    this.clipboard.url = URL.createObjectURL(data)
                    this.dialog.clipboard = true
                } catch (e) {
                    this.showTips('粘贴板图片读取权限被拒绝', Ws.error)
                }
            }
        },

        // 初始化emoji表情
        initEmojiMap() {
            axios.get('/css/emoji.base64').then(response => {
                this.emojiMap = JSON.parse(decodeURIComponent(atob(response.data)))
            })
        },

        // 初始化公告内容
        initAnnounce() {
            const digest = localStorage.getItem(Ws.announce)
            const owner = this.myself.isowner()
            axios.get('/user/notice', {
                params: {
                    digest: owner ? null : digest
                }
            }).then(response => {
                const data = response.data
                if (data.success) {
                    const announce = data.data
                    this.showAnnounce(announce.content, Wss.formatTime(announce.time))
                }
            })
        },

        // 添加图片加载完成动画
        imageLoaded(event) {
            const selecter = event.path.first()
            this.$nextTick(() => selecter.classList.add('img-loaded'))
        },

        // 初始化WebRTC本地连接约定
        initMediaPermission(stream, type, target) {
            const client = this.webrtc.client
            const user = this.webrtc.user = this.finduser(target || this.setting.target)
            this.webrtc.type = type
            this.webrtc.typeName = type === Ws.video ? '视频通话' : '语音通话'
            this.webrtc.startBtn = '发起'
            // 写入媒体权限
            client.initMediaStream(stream)
            // 初始化连接
            const candidate = ice => this.sendsys(user.uid, Ws.candidate, null, ice)
            const ready = () => target && this.sendsys(user.uid, type, Ws.accept)
            const done = () => {
                Object.values(this.webrtc.timer).forEach(t => clearTimeout(t))
                // 更新状态
                this.loads.call = false
                this.disabled.call = true
                this.webrtc.connected = 0
                this.webrtc.startBtn = '已连接'
                this.media.phone.stop()
                // 震动（如果有）
                this.setting.p_vibrate && window.navigator.vibrate([200, 100, 200])
                // 连接计时器
                this.webrtc.timer.interval = setInterval(() => {
                    this.webrtc.connectionTime = Wss.formatSeconds(this.webrtc.connected++)
                }, 1000)
            }
            client.initPeerConnection(candidate, ready, done)
        },

        initWebSocket() {
            const client = this.room.client = new WebSocket(Wss.getWsURL('/user/room'))
            // 加入聊天室
            client.onopen = () => {
                this.input.disabled = false
                this.input.placeholder = this.input.deftips
            }
            // 消息接收与处理
            client.onmessage = response => {
                const wsmsg = JSON.parse(response.data.decrypt())
                wsmsg.sysmsg ? this.parsemsg(wsmsg) : this.pushmsg(wsmsg)
            }
            // 服务器断开操作
            client.onclose = async event => {
                Loading.remove()
                this.logout(event.reason)
            }
        },

        // 初始化观察者
        initResizeObserver() {
            const aside = $('.el-aside'), chatbox = $('.lite-chatbox')
            // 侧边尺寸监视器
            new ResizeObserver(() => this.closePopover()).observe(aside)
            // 图片缩放监视器
            let state = aside.offsetWidth > 10
            new ResizeObserver(() => {
                if (state && aside.offsetWidth > 10 || !state && aside.offsetWidth < 10) return
                state = aside.offsetWidth > 10
                const messages = this.room.user.messages || []
                for (const wsmsg of messages.filter(wsmsg => wsmsg.isimage || wsmsg.isvideo)) {
                    wsmsg.data.random = Math.random()
                }
            }).observe(chatbox)
        },

        // 检查滚动位置是否在底部
        isBottom() {
            const scroll = this.scroll
            return scroll.scrollHeight - ~~(scroll.scrollTop + scroll.offsetHeight) < 10
        },

        // 检查按钮是否禁用
        isDisabledBottom() {
            return this.input.disabled || this.loads.record || this.media.audioBlob
        },

        // 检查未读语音
        isUnreadAudio(wsmsg) {
            wsmsg = wsmsg || this.cache.message
            return !wsmsg.isself && wsmsg.isaudio && wsmsg.unread
        },

        // 限制指定用户
        limitUser(cancel) {
            const limit = cancel ? 0 : this.menu.limit.time * this.menu.limit.unit
            this.sendsys(this.cache.user.uid, this.menu.limit.type, limit)
            this.dialog.limit = false
        },

        // 加载聊天记录
        loadMessage(bottom) {
            const room = this.room
            const messages = room.user.messages
            const scroll = this.scroll
            // 关闭所有popover
            this.closePopover()
            // 更多消息页数条件（滚动条在顶部 && [消息为空 || >=页消息数]）
            const hasNextPage = () => {
                // 可能为空 (未登录)
                if (messages == null) return false
                const len = messages.length
                const top = scroll.scrollTop
                return room.hasNext && top === 0 && (len === 0 || len >= Ws.PageSize)
            }
            return new Promise((resolve, reject) => {
                // 检查状态
                if (this.loads.page || !hasNextPage()) {
                    resolve()
                    return
                }
                this.closePopover()
                const lastId = messages.filter(e => !e.sysmsg).first()?.mid
                // 记录当前滚动总长度
                const height = scroll.scrollHeight
                // 获取消息列表
                this.loads.page = true
                axios.get('/message', {
                    params: {
                        mid: lastId,
                        target: room.user.uid
                    }
                }).then(response => {
                    const list = response.data.data
                    if (list.length < Ws.PageSize) {
                        room.hasNext = false
                        if (!list.length) {
                            resolve()
                            return
                        }
                    }
                    // 绑定消息发起者
                    const records = list.map(msg => JSON.parse(msg.decrypt())).map(msg => this.bindmsg(msg)).filter(e => e != null)
                    // 写入消息
                    for (const message of records) {
                        // 插入时间（从头判断）
                        if (Wss.isExpired(messages, 600, message.createTime)) {
                            const sysmsg = this.buildsys(Wss.formatTime(messages[0].createTime, true), Ws.time)
                            messages.unshift(sysmsg)
                        }
                        messages.unshift(message)
                    }
                    // 修改滚动条位置
                    this.$nextTick(() => {
                        this.scrollBottom(scroll.scrollHeight - (bottom || height))
                        resolve()
                    })
                }).catch((error) => {
                    if (error.response.status === 403) {
                        this.logout('登录信息过期')
                    }
                    reject()
                }).finally(() => {
                    this.loads.page = false
                })
            })
        },

        // 退出登录
        logout(reason) {
            axios.post('/user/logout').then(async response => {
                if (response.data.success) {
                    this.showTips('您已成功退出')
                    await Wss.sleep(500)
                }
            }).catch(_ => null).finally(() => {
                this.input.placeholder = reason
                this.input.disabled = this.dialog.login = true
                this.room.client?.close()
                CryptoJS.exchange()
                // 清除页面数据
                this.room.user = this.myself = {}
                this.userList = []
                this.room.online = 0
            })
        },

        // 监听输入框 @弹窗提示
        monitorInput(input) {
            if (!this.room.user.isgroup) return
            const close = () => {
                this.input.popper = false
                this.input.filter = ''
            }
            // 没有输入内容
            !input && close()
            // @开头 弹出用户选择
            if (input.startsWith('@')) {
                this.input.popper = true
                const search = input.substr(1)
                this.input.filter = ''
                if (search) {
                    const users = this.userList.filter(e => !e.isgroup)
                    const filter = users.map(e => e.name).concat(users.map(e => e.uid))
                    if (!filter.find(e => e.includes(search))) {
                        close()
                        return
                    }
                    this.input.filter = search
                }
                this.$refs.tree.filter()
                return
            }
            // 其他字符开头
            close()
        },

        // 插入要@人的昵称
        insertName(data) {
            this.input.content = '@' + data.name + ' '
            this.input.filter = ''
            this.input.popper = false
            this.$refs.input.select()
        },

        // 内置登录入口
        userLogin() {
            this.$refs.login.validate(valid => {
                if (valid) {
                    this.loads.optional = true
                    const data = this.login
                    axios.get('/api/login', {
                        params: CryptoJS.encrypt({
                            auto: data.auto,
                            user: data.user,
                            pass: data.pass,
                            code: data.code
                        }, {
                            pass: {encrypt}
                        })
                    }).then(res => {
                        const json = res.data
                        if (json.success) {
                            this.showTips(json.message, Ws.success)
                            Wss.sleep(500).then(() => {
                                this.dialog.login = false
                                this.initWebSocket()
                            })
                            return
                        }
                        // 异地登录验证
                        if (json.code === 4003) {
                            this.input.placeholder = `为了您的账号安全，请验证绑定邮箱（${json.message}）`
                            this.login.offsite = true
                            return
                        }
                        this.showTips(json.message, Ws.error)
                    }).catch(() => {
                        this.showTips('服务器繁忙，请稍后再试', Ws.error)
                    }).finally(() => {
                        this.loads.optional = false
                    })
                }
            })
        },

        // 内置注册入口
        userRegister() {
            this.$refs.register.validate(valid => {
                if (valid) {
                    this.loads.optional = true
                    axios.post('/api/register', CryptoJS.encrypt({
                        name: this.formData.name,
                        email: this.formData.email,
                        pass: this.formData.pass,
                        code: this.formData.code
                    }, {
                        pass: {encrypt}
                    })).then(response => {
                        const data = response.data
                        if (data.success) {
                            Wss.sleep(500).then(() => {
                                this.dialog.login = this.dialog.register = false
                                this.initWebSocket()
                            })
                        }
                        this.showTips(data.message, data.success)
                    }).catch((e) => {
                        this.showTips('服务器繁忙，请稍后再试', Ws.error)
                    }).finally(() => {
                        this.loads.optional = false
                    })
                }
            })
        },

        // 发送验证码
        sendEmail(user) {
            if (!user) return
            this.loads.sending = true
            axios.post('/api/send', CryptoJS.encrypt({
                user: user
            })).then(response => {
                const data = response.data
                this.showTips(data.success ? `已向${data.data}发送验证码` : data.message, data.success)
            }).catch(() => {
                this.showTips('服务器繁忙，请稍后再试', Ws.error)
            }).finally(() => {
                this.loads.sending = false
            })
        },

        // 修改密码
        updatePassword() {
            this.$refs.password.validate(valid => {
                if (valid) {
                    this.loads.optional = true
                    axios.post('/api/password', CryptoJS.encrypt({
                        email: this.myself.email || this.formData.email,
                        code: this.formData.code,
                        password: this.formData.pass
                    }, {
                        password: {encrypt}
                    })).then(response => {
                        const data = response.data
                        if (data.success) {
                            this.dialog.password = false
                            this.myself.email && this.logout()
                        }
                        this.showTips(data.message, data.success)
                    }).catch(() => {
                        this.showTips('服务器繁忙，请稍后再试', Ws.error)
                    }).finally(() => {
                        this.loads.optional = false
                    })
                }
            })
        },

        // 修改邮箱
        updateEmail() {
            this.$refs.email.validate(valid => {
                if (valid) {
                    this.loads.optional = true
                    axios.post('/user/email', CryptoJS.encrypt({
                        email: this.formData.email,
                        selfcode: this.formData.code,
                        newcode: this.formData.newcode
                    })).then(response => {
                        const data = response.data
                        if (data.success) {
                            this.dialog.password = false
                            this.myself.email && this.logout()
                        }
                        this.showTips(data.message, data.success)
                    }).catch(() => {
                        this.showTips('服务器繁忙，请稍后再试', Ws.error)
                    }).finally(() => {
                        this.loads.optional = false
                    })
                }
            })
        },

        // 打开语音通话窗口
        openAudioDialog(target) {
            if (!this.setting.phone) {
                return void this.showTips('您未启用通话功能，请前往设置开启')
            }
            if (this.webrtc.connected) {
                return void this.showTips('当前正在通话中', Ws.warning)
            }
            this.loads.stream = true
            Wss.getUserMedia(Ws.audio).then(async stream => {
                await this.stopAudio()
                await this.closeRemote()
                this.webrtc.client = new WebRTC()
                this.initMediaPermission(stream, Ws.audio, target)
                this.webrtc.dialog.audio = true
            }).catch(() => {
                this.showTips('媒体授权被拒绝', Ws.error)
            }).finally(() => {
                this.loads.stream = false
            })
        },

        // 打开用户限制窗口
        openLimit(type) {
            const limit = this.menu.limit
            limit.type = type
            limit.typeName = limit.type === Ws.mute ? '禁言' : '限制登录'
            this.changeTimeUnit()
            this.dialog.limit = true
        },

        // 查看资料
        openMaterial(uid) {
            const user = this.cache.user = this.finduser(uid)
            axios.get('/user/' + user.uid).then(response => {
                const material = response.data.data
                Object.keys(material).forEach(key => user[key] = material[key])
            })
            this.dialog.material = true
        },

        // 打开消息右键菜单
        async openMsgMenu(message) {
            const open = this.popover.message
            this.closePopover()
            open && await Wss.sleep(200)
            this.cache.message = message
            this.popover.message = true
        },

        // 桌面消息通知
        openNotify(group) {
            if (!window.Notification) {
                return void this.showTips('您的浏览器不支持此功能', Ws.error)
            }
            if (!group && this.setting.notify || group && this.setting.g_notify) {
                this.setting[group ? 'g_notify' : 'notify'] = false
                return
            }
            Notification.requestPermission().then(permission => {
                if (permission === "granted") {
                    group ? (this.setting.g_notify = true) : (this.setting.notify = true)
                    return
                }
                this.showTips('通知授权被拒绝', Ws.error)
            })
        },

        // 打开视频录制窗口（再次调用此方法切换后置摄像头[非录制状态]）
        openRecordDialog() {
            const recorder = this.media.recorder.video
            if (recorder.run || this.media.videoBlob) return
            this.loads.stream = true
            recorder.dispose()
            recorder.initMediaStream(this.webrtc.camera).then(async stream => {
                this.webrtc.camera = !this.webrtc.camera
                this.dialog.recorder = true
                await Wss.sleep(200)
                const video = $('#recorder-video')
                video.muted = true
                video.srcObject = stream
                video.play()
            }).catch(() => {
                this.showTips('媒体授权被拒绝', Ws.error)
            }).finally(() => {
                this.loads.stream = false
            })
        },

        // 打开用户右键操作
        async openUserMenu(uid, event) {
            if (uid === Ws.group || uid === this.myself.uid)
                return
            const open = this.menu.show
            this.closePopover()
            open && await Wss.sleep(50)
            this.cache.user = this.finduser(uid)
            // 计算高度
            const viewHeight = 280
            const offsetY = window.innerHeight - event.clientY < viewHeight
            const viewWidth = 145
            const offsetX = window.innerWidth - event.clientX < viewWidth
            this.menu.top = offsetY ? event.clientY - viewHeight : event.clientY
            this.menu.left = offsetX ? event.clientX - viewWidth : event.clientX
            this.menu.show = true
        },

        // 打开视频通话窗口
        openVideoDialog(target) {
            if (!this.setting.phone) {
                return void this.showTips('您未启用通话功能，请前往设置开启')
            }
            if (this.webrtc.connected) {
                return void this.showTips('当前正在通话中', Ws.warning)
            }
            this.loads.stream = true
            Wss.getUserMedia([Ws.video, Ws.audio]).then(async stream => {
                await this.closeRemote()
                await this.stopAudio()
                this.webrtc.dialog.video = true
                await Wss.sleep(200)
                this.webrtc.client = new WebRTC($('#local-video'), $('#remote-video'))
                this.initMediaPermission(stream, Ws.video, target)
            }).catch(() => {
                this.showTips('媒体授权被拒绝', Ws.error)
            }).finally(() => {
                this.loads.stream = false
            })
        },

        // 解析表情符号
        parseEmoji(msg) {
            const url = "https://twemoji.maxcdn.com/v/latest/svg/"
            const map = this.emojiMap
            const full = e => `<img class="emoji" src="${url}${e}"/>`
            return Array.from(msg).map(e => map[e] ? full(e.replace(e, map[e])) : e).join('')
        },

        // 解析角色
        parseRole: (user) => user.isowner() ? '所有者' : user.isadmin() ? '管理员' : '标准用户',
        // 解析时间
        parseTime: (time) => Wss.formatTime(time, true),
        // 解析消息类型
        parseType: (wsmsg) => wsmsg.isaudio ? '语音消息' : wsmsg.isimage ? '图片消息' : wsmsg.isvideo ? '视频文件' : wsmsg.isblob ? '文件' : '文字消息',

        // 解析系统消息
        parsemsg({uid, data, content, type, target}) {
            // 角色判定快接入口
            const initRole = user => {
                user.isowner = () => user.role === Ws.owner
                user.isadmin = () => user.role === Ws.admin || user.isowner()
                user.isuser = () => user.role === Ws.user
            }
            // 命令类型
            switch (type) {
                // 聊天初始化
                case Ws.init:
                    // 查找自己
                    this.myself = data.find(user => user.shields)
                    this.myself.self = true
                    // 写入用户列表
                    this.userList.push(this.groupData)
                    for (const user of data) {
                        initRole(user)
                        user.online && this.room.online++
                        if (user.shields) continue
                        // 初始化双向绑定属性
                        user.messages = []
                        user.unreadCount = user.unreadCount || 0
                        user.preview = user.preview || null
                        user.lastTime = Wss.formatTime(user.lastTime || user.loginTime)
                        this.userList.push(user)
                    }
                    // 判断保存的会话目标是否为自己
                    const setting = this.setting
                    this.switchSession(this.finduser(setting.target))
                    // 排序
                    this.sortList()
                    // 等待背景图片加载完成
                    Loading.show('加载背景图片')
                    Wss.loadImage(setting.bgURL).finally(() => {
                        Loading.remove()
                        // 弹出公告
                        this.initAnnounce()
                    })
                    break;
                // 用户加入（可能来自注册）
                case Ws.join:
                    let join = this.finduser(data.uid)
                    data.messages = []
                    initRole(data)
                    join == null ? this.userList.push(data) : (join.online = true)
                    join = join || data
                    join.lastTime = Wss.formatTime(data.lastTime)
                    // 更新基础数据
                    join.name = data.name
                    join.headimgurl = data.headimgurl
                    join.platform = data.platform
                    // 统计变动
                    this.room.online++
                    this.setting.join && this.showTips(content)
                    this.sortList()
                    break;
                // 用户退出
                case Ws.exit:
                    const exit = this.finduser(data.uid)
                    exit.online = false
                    this.room.online--
                    this.setting.exit && this.showTips(content, Ws.warning)
                    this.sortList()
                    this.parsemsg({uid: uid, type: Ws.leave, data: data})
                    break
                // 通话关闭
                case Ws.leave:
                    // 对比通话用户与退出用户
                    if (this.webrtc.user.uid !== (uid || data?.uid))
                        break;
                    // 关闭接听对话框
                    this.webrtc.dialog.phone = false
                    this.media.phone.stop()
                    // 检查通话发起
                    this.loads.call && this.stopWaiting()
                    // 检查通话连接
                    this.webrtc.connected && this.closeRemote()
                    break;
                // 禁言
                case Ws.mute:
                    const input = this.input
                    let time = data
                    // 清理定时器
                    clearInterval(input.intervar)
                    clearTimeout(input.timer)
                    if (!time) {
                        input.disabled = false
                        input.placeholder = input.deftips
                        this.pushsys(content)
                        break;
                    }
                    // 开始禁言流程
                    this.pushsys(content, Ws.warning)
                    input.disabled = true
                    // 超过禁言时间自动解除
                    input.timer = setTimeout(() => {
                        clearInterval(input.intervar)
                        input.disabled = false
                        input.placeholder = input.deftips
                    }, time * 1000)
                    // 按秒执行后台任务
                    input.intervar = setInterval(() => {
                        input.placeholder = `您已被禁言 (剩余${Wss.parseTime(time--)})`
                    }, 1000)
                    break;
                // 设为/取消管理员状态切换
                case Ws.role:
                    const ruser = this.finduser(data.uid)
                    ruser.role = data.role
                    this.pushsys(content, Ws.danger)
                    break;
                // 用户屏蔽回调
                case Ws.shield:
                    const shields = this.myself.shields
                    const index = shields.indexOf(data.uid);
                    ~index ? shields.splice(index, 1) : shields.push(data.uid)
                    this.showTips(content, Ws.info)
                    break;
                // 消息撤回
                case Ws.remove:
                    const self = uid === this.myself.uid
                    const group = target === Ws.group
                    const wuser = this.finduser(group ? Ws.group : uid)
                    // 显示消息
                    const tips_w = self ? '你' : group ? `${this.finduser(uid).name} [${uid}] ` : '对方'
                    const sys = this.buildsys(tips_w + '撤回了一条消息', Ws.success)
                    // 替换消息
                    this.replaceMsg(content, sys, wuser.messages)
                    // 更新消息预览
                    if (target) {
                        const wuser = this.finduser(target === this.myself.uid ? uid : target)
                        wuser.preview = '[消息撤回]'
                        wuser.lastTime = Wss.formatTime()
                    }
                    break;
                // 公告提醒
                case Ws.announce:
                    this.showAnnounce(content, Wss.formatTime())
                    break
                // 语音/视频发起
                case Ws.audio:
                case Ws.video:
                    // 若反馈消息不是空（目标用户已应答）
                    if (content) {
                        // 若对方接受
                        if (content === Ws.accept) {
                            this.remotePeerConnection()
                            break;
                        }
                        this.stopWaiting()
                        this.showTips(content, Ws.warning)
                        break;
                    }
                    // 反馈消息为空 - 对方发起视频请求处理
                    if (content == null) {
                        // 检查功能
                        if (!this.setting.phone) {
                            this.sendsys(uid, type, '对方关闭了通话功能')
                            break;
                        }
                        // 查找发起者
                        const vuser = this.finduser(uid)
                        // 通话繁忙检查
                        const typeName = type === Ws.video ? '视频通话' : '语音通话'
                        const tips_v = `${vuser.name} [${vuser.uid}] 向您发起了${typeName}`
                        if (this.webrtc.connected || this.loads.call || this.webrtc.dialog.phone) {
                            this.showTips(tips_v)
                            this.sendsys(uid, type, '对方正忙，请稍后再试')
                            break;
                        }
                        // 同步数据
                        this.webrtc.user = vuser
                        this.webrtc.type = type
                        this.webrtc.typeName = typeName
                        // 弹窗提示（60秒关闭）
                        this.webrtc.dialog.phone = true
                        this.closeRemote().then(() => {
                            const phone = this.media.phone
                            this.setting.p_voice && phone.start(this.setting.p_voice_src || '/wav/call.wav').catch(() => {
                                this.showTips('来电铃声资源地址失效', Ws.warning)
                                phone.start('/wav/call.wav')
                            })
                            this.webrtc.timer.starter = setTimeout(() => {
                                this.webrtc.dialog.phone = false
                                phone.stop()
                            }, 60000)
                        })
                    }
                    break;
                // 更新用户头衔
                case Ws.alias:
                    const auser = this.finduser(target)
                    auser.alias = content
                    // 显示消息
                    const tips_a = `${target === this.myself.uid ? '您' : `${auser.name} [${auser.uid}] `} 已被所有者`
                    this.pushsys(tips_a + (content ? `授予 ${content} 头衔` : '取消头衔'), Ws.success)
                    break;
                // 远程连接信令
                case Ws.offer:
                    this.remotePeerConnection(content, uid)
                    break;
                // 远程连接应答
                case Ws.answer:
                    this.webrtc.client.setAnswer(content)
                    break;
                // 远程消息交换
                case Ws.candidate:
                    this.webrtc.client.addCandidate(data)
                    break;
                default:
                    this.pushsys(content, type)
            }
        },

        // 播放语音
        async playAudio(wsmsg) {
            // 录音状态检查
            if (this.media.recorder.audio.run) {
                return void this.showTips('录音状态下不支持操作', Ws.warning)
            }
            const player = this.media.audio
            const current = this.cache.audio
            // 正在播放则停止
            if (current.playing) {
                await this.stopAudio()
                // 操作源相同 终止继续执行
                if (current.mid === wsmsg.mid) {
                    return
                }
            }
            wsmsg.playing = true
            // 发送已读标记
            wsmsg.unread && wsmsg.isaudio && this.readAudioMsg(wsmsg)
            // 开始播放
            player.start('/resource/' + wsmsg.mid).catch(() => {
                this.showTips('媒体资源已过期', Ws.error)
            }).finally(() => {
                wsmsg.playing = false
            })
            // 替换当前语音数据
            this.cache.audio = wsmsg
        },

        // 播放视频
        async playVideo(wsmsg) {
            this.dialog.player = true
            await this.stopAudio()
            await Wss.sleep(200)
            let dp = this.media.DPlayer
            if (dp) {
                dp.switchVideo({url: '/resource/' + wsmsg.mid})
            } else {
                dp = this.media.DPlayer = new DPlayer({
                    element: $('#dplayer'),
                    loop: true,
                    autoplay: true,
                    video: {url: '/resource/' + wsmsg.mid},
                    contextmenu: [{
                        text: '缩放全屏',
                        click: e => {
                            const list = e.video.classList
                            list.contains('cover') ? list.remove('cover') : list.add('cover')
                        }
                    }]
                })
            }
            dp.seek(0)
            dp.video.play()
            // 重复事件过滤
            let error = true;
            dp.on('error', () => {
                error && this.showTips('媒体资源已过期', Ws.error)
                error = false
            })
        },

        // 推送公告
        pushAnnounce() {
            this.sendsys(this.myself.uid, Ws.announce, this.owner.announce)
            this.showTips('发布成功', Ws.success)
        },

        // 尝试打开文件
        async openBlob(wsmsg) {
            // 分割扩展名
            const index = wsmsg.content.lastIndexOf('.')
            const ext = index > -1 ? wsmsg.content.substr(index + 1) : ''
            // 音乐
            if (['mp3', 'wav', 'ogg'].includes(ext)) {
                return void this.playAudio(wsmsg)
            }
            this.saveBlob(wsmsg)
        },

        // 写入用户消息
        pushmsg(wsmsg) {
            // 构建消息
            wsmsg = this.bindmsg(wsmsg)
            // 消息提示
            if (!wsmsg.isself && !wsmsg.sysmsg) {
                if (wsmsg.isgroup) {
                    this.setting.g_tips && this.setting.target !== Ws.group && this.showTips(`群组 [${wsmsg.user.name}]：${wsmsg.preview}`)
                    this.setting.g_voice && !this.loads.call && this.media.notice.start('/wav/notification.wav')
                    this.setting.g_vibrate && window.navigator.vibrate([200, 100, 200])
                    this.setting.g_notify && Wss.showNotify(wsmsg.user.name, wsmsg.preview)
                } else {
                    this.setting.tips && !wsmsg.istarget && this.showTips(`${wsmsg.user.name}：${wsmsg.preview}`)
                    this.setting.voice && !this.loads.call && this.media.notice.start('/wav/notification.wav')
                    this.setting.vibrate && window.navigator.vibrate([200, 100, 200])
                    this.setting.notify && Wss.showNotify(wsmsg.user.name, wsmsg.preview)
                }
            }
            // 查找消息目标
            const target = this.finduser(wsmsg.savepath)
            // 若消息已存在仅更新内容（mid必须存在）
            const findmsg = target.messages.filter(e => e.mid === wsmsg.mid).first()
            if (findmsg) {
                // 图片url不替换
                findmsg.isimage || findmsg.isvideo || (findmsg.content = wsmsg.content)
                findmsg.reject = wsmsg.reject
                return
            }
            // 插入时间（超过10分钟）
            if (target.messages && Wss.isExpired(target.messages, 600)) {
                this.pushsys(Wss.formatTime(Date.now(), true), Ws.time, wsmsg.savepath)
            }
            // 写入消息
            return this.writemsg(wsmsg)
        },

        // 写入系统消息
        pushsys(content, type, target) {
            this.writemsg(this.buildsys(content, type, target || this.setting.target))
        },

        // 写入到消息列表
        writemsg(wsmsg) {
            // 记录滚动位置（如果是自己发送的或滚动条在末尾，则滚动新消息）
            const isbottom = wsmsg.isself || this.isBottom()
            const user = this.finduser(wsmsg.sysmsg ? wsmsg.target : wsmsg.savepath)
            // 侧边栏消息设置（不显示系统消息）
            if (!wsmsg.sysmsg) {
                user.preview = wsmsg.preview
                user.lastTime = Wss.formatTime()
                !wsmsg.istarget && !wsmsg.isgroup && user.unreadCount < 100 && user.unreadCount++
            }
            // 写入消息列表
            if (wsmsg.istarget || user.messages.length > 0) {
                user.messages.push(wsmsg)
            }
            // 操作滚动条
            isbottom && this.$nextTick(() => this.scrollBottom())
            return user.messages.last()
        },

        // 已读指定用户所有消息
        readAllMsg() {
            const user = this.finduser(this.cache.user.uid)
            axios.post('/message/reading', {
                target: user.uid,
                sign: CryptoJS.sign(user.uid)
            }).then(() => {
                user.unreadCount = 0
                user.preview = null
                user.messages.filter(m => m.isaudio).forEach(m => m.unread = false)
            })
        },

        // 设置指定语音消息已读
        readAudioMsg(wsmsg) {
            wsmsg = wsmsg || this.cache.message
            axios.post('/message/reading', CryptoJS.encrypt({
                mid: wsmsg.mid,
                target: wsmsg.uid
            }))
            wsmsg.unread = false
        },

        // 显示公告弹窗
        showAnnounce(content, time) {
            // 所有者
            if (this.myself.isowner()) {
                this.owner.announce = content
                return
            }
            // 弹出公告
            window.alert(content, {
                title: '公告 [' + time + ']',
                confirmButtonText: '知道了',
                type: 'info'
            }).then(() => {
                // 散列最新公告内容
                const digset = CryptoJS.MD5(content).toString()
                localStorage.setItem(Ws.announce, digset)
            })
        },

        // 拒绝远程连接
        rejectRemote(shield) {
            const target = this.webrtc.user.uid
            this.sendsys(target, this.webrtc.type, '对方拒绝了您的通话请求')
            shield && this.shieldUser(target)
            this.media.phone.stop()
            this.webrtc.dialog.phone = false
        },

        // 对等连接入口
        async remotePeerConnection(offer, target) {
            // 远程连接状态更新
            this.loads.call = true
            this.webrtc.startBtn = '连接中'
            this.media.phone.stop()
            Object.values(this.webrtc.timer).forEach(t => clearTimeout(t))
            this.webrtc.timer.timeout = setTimeout(() => {
                this.loads.call = false
                this.webrtc.startBtn = '发起'
                this.showTips('服务器繁忙，请稍后再试', Ws.warning)
            }, 5000)
            // 发起信令
            const client = this.webrtc.client
            const user = this.webrtc.user
            const sdp = offer ? await client.createAnswer(offer) : await client.createOffer()
            // 目标为空表示发起信令，反之表示发起应答
            this.sendsys(target || user.uid, target ? Ws.answer : Ws.offer, sdp)
        },

        // 移除消息
        removeMsg(wsmsg) {
            wsmsg = wsmsg || this.cache.message
            wsmsg.reject && this.withdrawMsg(wsmsg.mid)
            this.replaceMsg(wsmsg.mid)
            wsmsg.mid === this.cache.audio.mid && this.stopAudio()
        },

        // 系统管理员移除消息
        removeMsg2() {
            const mid = this.owner.mid
            if (!mid) return
            this.loads.rm_msg = true
            axios.post('/message/remove', CryptoJS.encrypt({
                mid: mid
            })).then(response => {
                const data = response.data
                this.showTips(data.message, data.success)
                data.success && (this.owner.mid = '')
            }).catch(() => {
                this.showTips('服务器繁忙，请稍后再试', Ws.error)
            }).finally(() => {
                this.loads.rm_msg = false
            })
        },

        // 系统管理员移除用户
        removeUser() {
            const uid = this.owner.uid
            if (!uid) return
            this.loads.rm_user = true
            axios.post('/user/remove', CryptoJS.encrypt({
                uid: uid
            })).then(response => {
                const data = response.data
                if (data.success) {
                    const user = this.finduser(uid)
                    user.online && this.sendsys(user.uid, Ws.lock, Ws.forever)
                    this.owner.uid = ''
                }
                this.showTips(data.message, data.success)
            }).catch(() => {
                this.showTips('服务器繁忙，请稍后再试', Ws.error)
            }).finally(() => {
                this.loads.rm_user = false
            })
        },

        // 移除/替换消息（仅本地）
        replaceMsg(mid, replace, message) {
            const list = message || this.room.user.messages
            for (let i = 0; i < list.length; i++) {
                if (list[i].mid === mid) {
                    const arr = [i, 1]
                    replace != null && arr.push(replace)
                    list.splice.apply(list, arr)
                    // 移除重叠时间
                    if (replace == null) {
                        const prev = list[i - 1], next = list[i]
                        if (prev?.type === Ws.time && (next == null || next?.type === Ws.time)) {
                            list.splice(i - 1, 1)
                        }
                    }
                    break
                }
            }
        },

        // 还原设置
        restoreSetting() {
            const setting = JSON.parse(localStorage.getItem(Ws.setting))
            this.setting = setting ? setting : this.setting
            this.setting.target = setting?.target || Ws.group
            setting?.bgURL && this.setBackground(setting.bgURL, true)
            setting?.bubble && this.changeBubble(setting.bubble, true)
        },

        // 滚动消息到指定位置（默认底部）
        scrollBottom(offset) {
            this.$refs.scroll.setScrollTop(offset || this.scroll.scrollHeight)
        },

        // 发送语音
        async sendAudio() {
            const record = this.media.audioBlob
            const wsmsg = this.pushmsg(this.buildmsg(null, Ws.audio, {time: record.time}))
            axios.post('/resource/audio', JSON.toForm({
                blob: record.blob,
                mid: wsmsg.mid,
            })).then(response => {
                const data = response.data
                if (data.success) {
                    this.sendmsg(null, Ws.audio, {time: record.time}, wsmsg.mid)
                }
            }).finally(() => {
                this.clearAudio()
            })
        },

        // 上传图片/视频/文件
        async uploadBlob(blob) {
            // 禁言检查
            if (this.input.disabled) return
            // 选择文件
            try {
                blob = blob || await Wss.chooseFile(30 * 1024 * 1024)
            } catch (e) {
                return void this.showTips(e, Ws.error)
            }
            // 图片
            if (blob.type.startsWith('image')) {
                // 可能来着粘贴板
                this.dialog.clipboard = false
                // 压缩图片
                this.loads.upload = true
                blob = await Wss.zipImage(blob, 3 * 1024 * 1024)
                this.loads.upload = false
                // 获取图片尺寸
                const url = this.clipboard.url || URL.createObjectURL(blob)
                const size = await Wss.loadImage(url)
                const wsmsg = this.pushmsg(this.buildmsg(url, Ws.img, size))
                // 上传图片
                axios.post('/resource/image', JSON.toForm({
                    blob: blob,
                    mid: wsmsg.mid,
                }), {
                    onUploadProgress: event => {
                        wsmsg.progress_number = event.loaded / event.total * 100 | 0
                        wsmsg.progress = (100 - wsmsg.progress_number) + '%'
                    }
                }).then(async response => {
                    const data = response.data
                    if (data.success) {
                        const imgURL = '/resource/' + wsmsg.mid
                        this.sendmsg(imgURL, Ws.img, size, wsmsg.mid)
                        // 真实图片URL将在切换会话时替换
                        this.room.cache.push({mid: wsmsg.mid, url: imgURL, blob: url})
                        return
                    }
                    throw data.message
                }).catch(e => {
                    this.pushsys(e || '由于网络原因，文件发送失败', Ws.danger)
                    wsmsg.reject = true
                }).finally(() => {
                    wsmsg.progress = null
                })
                return
            }
            // 文件大小检查
            if (blob.size > 30 * 1024 * 1024) {
                return void this.showTips('文件大小超过30MB限制', Ws.warning)
            }
            // 视频
            if (blob.type.startsWith('video')) {
                // 获取视频数据
                const video = await Wss.loadVideo(URL.createObjectURL(blob))
                const duration = blob.duration || (video.duration ? Wss.formatSeconds(video.duration) : '')
                const data = {width: video.width, height: video.height, duration: duration}
                // 抽取视频第一帧
                const blobURL = URL.createObjectURL(video.frame)
                const wsmsg = this.pushmsg(this.buildmsg(blobURL, Ws.video, data))
                // 先上传预览图
                wsmsg.progress_number = 0
                wsmsg.progress = '100%'
                axios.post('/resource/image', JSON.toForm({
                    blob: video.frame,
                })).then(async response => {
                    let idata = response.data
                    if (!idata.success) throw idata.message
                    // 视频预览图URL
                    const imgURL = idata.data
                    // 上传视频
                    try {
                        response = await axios.post('/resource/blob', JSON.toForm({
                            blob: blob,
                            mid: wsmsg.mid,
                        }), {
                            onUploadProgress: event => {
                                wsmsg.progress_number = event.loaded / event.total * 100 | 0
                                wsmsg.progress = (100 - wsmsg.progress_number) + '%'
                            }
                        })
                    } catch (e) {
                        throw '由于网络原因，文件发送失败'
                    }
                    const vdata = response.data
                    if (vdata.success) {
                        this.sendmsg(imgURL, Ws.video, data, wsmsg.mid)
                        this.room.cache.push({mid: wsmsg.mid, url: imgURL})
                        return
                    }
                    throw vdata.message
                }).catch(e => {
                    this.pushsys(e || '由于网络原因，文件发送失败', Ws.danger)
                    wsmsg.reject = true
                }).finally(() => {
                    wsmsg.progress = null
                })
                return
            }
            // 上传文件
            const size = {size: (Math.round(blob.size / 1024 / 1024 * 100) / 100 || 0.01) + 'MB'}
            const wsmsg = this.pushmsg(this.buildmsg(blob.name, Ws.blob, size))
            // 检查服务器文件是否存在
            const digest = JSON.toForm({digest: await Wss.digestBlob(blob), mid: wsmsg.mid})
            if ((await axios.post('/resource/blob', digest)).data.success) {
                this.sendmsg(blob.name, Ws.blob, size, wsmsg.mid)
                return
            }
            // 上传开始
            axios.post('/resource/blob', JSON.toForm({
                blob: blob,
                mid: wsmsg.mid,
            }), {
                onUploadProgress: event => {
                    wsmsg.progress_number = event.loaded / event.total * 100 | 0
                    wsmsg.progress = wsmsg.progress_number + '%'
                }
            }).then(response => {
                const data = response.data
                if (data.success) {
                    this.sendmsg(blob.name, Ws.blob, size, wsmsg.mid)
                    return
                }
                throw data.message
            }).catch(e => {
                this.pushsys(e || '由于网络原因，文件发送失败', Ws.danger)
                wsmsg.reject = true
            }).finally(() => {
                wsmsg.progress = null
            })
        },

        // 发送消息
        sendmsg(content, type = Ws.text, data, mid) {
            if (this.input.disabled) return
            // 检查屏蔽
            if (this.myself.shields.includes(this.setting.target)) {
                return void this.showTips('您已屏蔽当前用户', Ws.warning)
            }
            this.input.content = ''
            // 语言发送 / 录制
            if (type === Ws.text && !content) {
                this.media.audioBlob ? this.sendAudio() : this.startRecord()
                return
            }
            // 发送消息
            const wsmsg = this.buildmsg(content, type, JSON.stringify(data), mid)
            this.room.client.send(JSON.stringify(wsmsg).encrypt())
            this.pushmsg(wsmsg)
        },

        // 发送系统消息
        sendsys(target, wstype, content, data) {
            this.room.client.send(JSON.stringify(this.buildsys(content, wstype, target, data)).encrypt())
        },

        // 设置背景图片
        async setBackground(url, init) {
            const apply = url => {
                document.body.style.setProperty('--bg', `url('${url}')`)
                init || this.showTips('设置成功')
                // 同步bgURL-input
                this.setting.bgURL = url
            }
            this.loads.upload = true
            // 先验证输入框内URL
            if (url) {
                // 是否来自blob映射
                if (url.startsWith('blob:')) {
                    url = this.room.cache.find(data => data.blob === url)?.url
                }
                try {
                    await Wss.loadImage(url)
                    return void apply(url)
                } catch (e) {
                    return void this.showTips('无效的背景图片URL地址')
                } finally {
                    this.loads.upload = false
                }
            }
            // 上传图片
            this.loads.upload = false
            let image = await Wss.chooseFile(30 * 1024 * 1024, Ws.img)
            this.loads.upload = true
            image = await Wss.zipImage(image, 3 * 1024 * 1024)
            axios.post('/resource/image', JSON.toForm({
                blob: image
            })).then(response => {
                const data = response.data
                data.success && apply(data.data)
            }).catch(reason => {
                !init && this.showTips(reason, Ws.error)
            }).finally(() => {
                this.loads.upload = false
            })
        },

        // 设置来电铃声
        async setRingtone(wsmsg) {
            let blob;
            const url = wsmsg ? '/resource/' + wsmsg.mid : URL.createObjectURL(blob = await Wss.chooseFile(10 * 1024 * 1024, Ws.audio))
            Wss.loadAudio(url).then(() => {
                // 若blob非空 先上传到服务器
                this.setting.p_voice_src = url
                this.setting.p_voice_src_name = wsmsg?.content || blob.name
                this.showTips('设置成功', Ws.success)
            }).catch(e => {
                this.showTips(e, Ws.error)
            })
        },

        // 清除来电铃声
        clearRingtone() {
            this.setting.p_voice_src_name = this.setting.p_voice_src = null
            this.showTips('已恢复默认铃声')
        },

        // 屏蔽指定用户
        shieldUser(uid) {
            this.sendsys(uid || this.cache.user.uid, Ws.shield)
        },

        // 提示消息
        showTips(content, type) {
            type = typeof type === 'boolean' ? type ? Ws.success : Ws.error : type || Ws.info
            this.globalTips?.close()
            this.globalTips = ElementPlus.ElMessage({
                message: content,
                type: type,
                showClose: true,
                duration: 3000
            })
        },

        // 用户列表排序
        sortList() {
            const tmp = this.userList
            this.userList = []
            tmp.sort((user1, user2) => {
                // 同时在线 || 同时不在线 按登录时间排序
                if (user1.online && user2.online || !user1.online && !user2.online) {
                    return user2.loginTime - user1.loginTime
                }
                // 在线排在不在线前
                return user1.online && !user2.online ? -1 : 1
            })
            this.$nextTick(() => this.userList = tmp)
        },

        // 录音
        async startRecord() {
            if (this.webrtc.connected) {
                return void this.showTips('正在通话中，无法录音', Ws.warning)
            }
            const recorder = this.media.recorder.audio
            // 保存
            const save = blob => {
                this.media.audioBlob = blob
                this.loads.record = false
                this.input.placeholder = '点击此处清除录音'
            }
            // 出错
            const error = reason => {
                this.showTips(reason, Ws.error)
                this.loads.record = this.input.disabled = false
                this.input.placeholder = this.input.deftips
            }
            // 停止录音
            if (recorder.run) {
                recorder.stop().then(() => save(recorder.getBlob())).catch(reason => error(reason))
                this.loads.record = false
                return
            }
            // 终止播放器
            await this.stopAudio()
            // 开始录音
            this.loads.record = true
            this.input.placeholder = '正在录音...'
            this.$refs.emoji?.hide()
            recorder.start().then(blob => save(blob)).catch(reason => error(reason))
        },

        // 录像
        startVideoRecord() {
            const video = $('#recorder-video')
            const recorder = this.media.recorder.video
            const blob = this.media.videoBlob
            // 缓存非空 - 发送视频
            if (blob) {
                // 写入录制时间（这个格式无法快速获取视频时间）
                blob.duration = this.media.recordTime
                this.uploadBlob(blob)
                this.disposeRecorder()
                return
            }
            // 录制中 - 停止
            if (recorder.run) {
                clearInterval(this.media.recordTimer)
                this.media.recording = false
                return void recorder.stop()
            }
            // 录制
            this.media.recording = true
            let time = 0
            this.media.recordTimer = setInterval(() => {
                this.media.recordTime = Wss.formatSeconds(++time)
            }, 1000)
            recorder.start().then(blob => {
                video.src = URL.createObjectURL(blob)
                video.muted = false
                video.play()
                this.media.videoBlob = blob
            })
        },

        // 发起远程会话
        startRemote(type) {
            // 检查离线
            if (!this.webrtc.user.online) {
                return void this.showTips('用户离线中')
            }
            // 检查屏蔽
            if (this.myself.shields.includes(this.setting.target)) {
                return void this.showTips('您已屏蔽当前用户', Ws.warning)
            }
            this.loads.call = true
            // 播放等待音
            this.media.phone.start('/wav/waiting.wav')
            this.webrtc.startBtn = '呼叫中'
            this.sendsys(this.webrtc.user.uid, type)
            // 远程发起计时器 60秒超时
            Object.values(this.webrtc.timer).forEach(t => clearTimeout(t))
            this.webrtc.timer.starter = setTimeout(() => {
                this.stopWaiting()
                this.showTips('对方未应答，请稍后再试', Ws.warning)
            }, 60000)
        },

        // 停止语音播放
        async stopAudio() {
            const player = this.media.audio
            this.cache.audio.playing = false
            if (player.run) {
                await player.stop()
            }
        },

        // 中断发起操作
        stopWaiting() {
            this.webrtc.startBtn = '发起'
            this.loads.call = false
            this.media.phone.stop()
            Object.values(this.webrtc.timer).forEach(t => clearTimeout(t))
        },

        // 前/后置摄像头切换
        switchCamera() {
            const client = this.webrtc.client
            client.switchCamera().then(state => {
                this.webrtc.camera = state
            }).catch(() => {
                this.showTips('媒体授权被拒绝', Ws.error)
            })
        },

        // 切换本地/远程视频窗口
        switchLocalRemote() {
            this.webrtc.client.switchLocalRemote()
        },

        // 切换最小化语音窗口
        switchMiniAudio(state) {
            this.webrtc.dialog.audio = !state
            this.mini.audio = !!state
            this.$nextTick(() => Wss.moveBind($('.mini-audio')))
        },

        // 切换最小化视频窗口
        switchMiniVideo(state) {
            const client = this.webrtc.client
            this.webrtc.dialog.video = !state
            this.mini.video = !!state
            this.$nextTick(() => {
                const mini = $('#mini-remote')
                mini && (mini.srcObject = state && client.remote.srcObject)
                mini.play()
                Wss.moveBind($('.mini-video'))
            })
        },

        // 切换会话
        switchSession(user) {
            // 切换前检查
            if (this.loads.page) {
                return void this.showTips('正在加载聊天记录...')
            }
            // 用户可能不存在（被注销）
            user = user || this.finduser(Ws.group)
            // 目标可能是自己（跳转到群组）
            if (user.uid === this.myself.uid) {
                user = this.finduser(Ws.group)
            }
            // 切换目标检查
            const choose = this.room.user
            if (choose?.uid !== user.uid) {
                this.closePopover()
                this.stopAudio()
                // 清理资源（可能为空 未登录）
                if (choose.messages) {
                    const messages = choose.messages = choose.messages.slice(-Ws.PageSize)
                    this.room.cache.forEach(data => {
                        const find = messages.find(wsmsg => wsmsg.mid === data.mid)
                        find && (find.content = data.url)
                    })
                }
                this.room.indexes = this.room.cache = []
                URL.revokeAllObjectURLs()
                // 更改当前会话用户
                this.room.user = user
                this.setting.target = user.uid
                this.room.hasNext = true
                // 同步服务器数据
                this.sendsys(user.uid, Ws.choose)
                user.unreadCount = 0
                user.preview = null
                // 检查缓存 加载聊天记录
                this.loads.animation = true
                // 等待切换动画
                this.$nextTick().then(async () => {
                    this.scrollBottom()
                    // 如果缓存消息小于页消息数，不会加载聊天记录
                    user.messages.length === Ws.PageSize || await this.loadMessage(true)
                    // 等待消息过渡动画
                    await Wss.sleep(300)
                    this.loads.animation = false
                })
            }
            // 侧边栏滑动事件
            ismobile && this.touch.right().then(() => this.scrollBottom())
        },

        // 设置管理员
        updateAdmin() {
            this.sendsys(this.cache.user.uid, Ws.role)
        },

        // 设置头衔
        updateAlias() {
            window.prompt('注：不输入任何内容即可取消头衔', '设置头衔', {
                inputPlaceholder: '请输入头衔 (2-5个字符长度)',
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'info'
            }).then(({value}) => {
                if (!/^[^\s]{2,5}$/.test(value)) {
                    return void this.showTips('头衔长度不合法', Ws.error)
                }
                this.sendsys(this.cache.user.uid, Ws.alias, value)
            })
        },

        // 修改头像
        updateAvatar() {
            if (!this.cache.user.self) return
            Wss.chooseFile(3000 * 1024, Ws.img).then(async blob => {
                blob = await Wss.zipImage(blob, 300 * 1024)
                this.loads.upload = true
                axios.post('/user/avatar', JSON.toForm({
                    blob: blob,
                    sign: await CryptoJS.signBlob(blob)
                })).then(response => {
                    const data = response.data
                    if (data.success) {
                        this.myself.headimgurl = this.cache.user.headimgurl = data.data
                    }
                    this.showTips(data.message, data.success)
                }).finally(() => {
                    this.loads.upload = false
                })
            }).catch(reason => {
                this.showTips(reason, Ws.error)
            })
        },

        // 修改资料
        updateMaterial() {
            const user = this.cache.user
            if (!user.name) {
                return void this.showTips('请输入昵称', Ws.warning)
            }
            this.loads.optional = true
            axios.post('/user/material', CryptoJS.encrypt({
                name: user.name,
                sex: user.sex,
                age: user.age,
                phone: user.phone,
                birth: user.birth
            })).then(response => {
                const data = response.data
                if (data.success) {
                    this.myself.name = user.name
                    this.dialog.material = false
                }
                this.showTips(data.message, data.success)
            }).catch(() => {
                this.showTips('服务器繁忙，请稍后再试', Ws.error)
            }).finally(() => {
                this.loads.optional = false
            })
        },

        // 预览图片
        viewImages(url) {
            this.media.viewer.view(this.getPreviewList(url))
        },

        // 撤回消息（如果消息未能送达 撤回一定成功）
        withdrawMsg(mid) {
            this.sendsys(this.setting.target, Ws.remove, mid || this.cache.message.mid)
        },

        // 阻止回退功能
        preventPopstate() {
            let state = true;
            history.pushState(null, null, window.location.href)
            window.onpopstate = () => {
                if (!state) return
                if (!Object.values(this.dialog).find(d => d)) {
                    this.touch?.right()
                    this.media.DPlayer?.pause()
                    this.media.viewer.close()
                }
                this.closePopover()
                Object.keys(this.dialog).filter(d => !['login'].includes(d)).forEach(e => this.dialog[e] = false)
                state = false
                setTimeout(() => state = true, 100)
                history.forward()
            }
        }
    },

    mounted() {
        // 初始化数据
        this.restoreSetting()
        this.initWebSocket()
        this.initEmojiMap()
        this.initDrogFile()
        this.initResizeObserver()
        this.preventPopstate()
        // 链接数据
        Object.keys(Ws).forEach(key => this.Ws[key] = Ws[key])
        Object.keys(Support).forEach(key => this.Support[key] = Support[key])
        this.scroll = this.$refs.scroll.wrap$
        this.reload = () => location.reload()
        // popover关闭方法注册
        $('.el-tree').onscroll = document.documentElement.onclick = () => this.closePopover()
        // 手机体验优化
        const input = $('.el-footer input')
        this.initClipboard(input)
        if (ismobile) {
            this.touch = Touchs.create($('aside'), $('section.is-vertical'))
            window.onresize = () => input === document.activeElement && this.scrollBottom()
        }
    },

    // 设置变动监听器
    watch: {
        'setting': {
            handler(setting) {
                localStorage.setItem(Ws.setting, JSON.stringify(setting))
            },
            deep: true
        },
        // 主题切换监控
        'setting.theme'(theme) {
            document.documentElement.setAttribute('class', theme ? 'dark' : 'light')
            theme || (this.setting.sub_theme = false)
        },
        // 子主题切换监控
        'setting.sub_theme'() {
            this.setting.sub_rgb = this.setting.sub_theme ? this.setting.bubble : ''
        },
        // 气泡颜色切换监控
        'setting.bubble'(bubble) {
            this.setting.sub_theme && (this.setting.sub_rgb = bubble)
        }
    }
})

// 覆盖基础提示框
window.alert = ElementPlus.ElMessageBox.alert
window.prompt = ElementPlus.ElMessageBox.prompt
window.confirm = ElementPlus.ElMessageBox.confirm
// 挂载数据
app.use(ElementPlus)
Object.entries(ElementPlusIconsVue).forEach(item => {
    const [key, component] = item
    const name = key.replace(/([A-Z])/g, "-$1").toLowerCase().substr(1)
    app.component(name, component)
})
app.mount('#app')
