// ws常量池
const Ws = {
    // 消息类型：文本
    text: 'text',
    // 消息类型：图片
    img: 'img',
    // 消息类型：文件
    blob: 'blob',
    // 所有者命令：切换管理员身份
    role: 'role',
    // 所有者命令：设置头衔
    alias: 'alias',
    // 管理员命令：禁言
    mute: 'mute',
    // 管理员命令：限制登陆
    lock: 'lock',
    // 用户命令：屏蔽
    shield: 'shield',
    // 用户命令：撤回/删除消息
    remove: 'remove',
    // 系统命令：加入的用户
    join: 'join',
    // 系统命令：退出的用户
    exit: 'exit',
    // 系统命令：列表选择同步
    choose: 'choose',
    // 系统命令：加载聊天室所有用户
    init: 'init',
    // 系统命令：加载聊天记录
    message: 'message',
    // 本地标识：公共群组
    group: 'Group',
    groupName: '公共聊天室',
    // 系统消息类型：主要
    primary: 'primary',
    // 系统消息类型：成功
    success: 'success',
    // 系统消息类型：警告
    warning: 'warning',
    // 系统消息类型：错误
    danger: 'danger',
    // 系统消息类型：错误
    error: 'error',
    // 系统消息类型：信息
    info: 'info',
    // 用户身份：标准用户
    user: 'user',
    // 用户身份：管理员
    admin: 'admin',
    // 用户身份：所有者
    owner: 'owner',
    // WebRTC会话标识
    candidate: 'candidate',
    answer: 'answer',
    offer: 'offer',
    leave: 'leave',
    // 公告标记
    announce: 'announce',
    // 媒体连接接受标识
    accept: 'accept',
    // 设备标识
    audio: 'audio',
    video: 'video',
    // 本地设置标记
    setting: "setting",
    // 时间标记
    time: 'time',
    // 一页消息数量（与服务器同步）
    PageSize: 25,
    // 强制下线标记
    forever: 'forever'
}

// 移动端列表滑动解决方案
const Touchs = {
    create: function (aside, container) {
        const touch = {}
        const offset = 50
        aside.style.width = '0px'

        // 向左滑动事件
        touch.left = async function () {
            aside.removeAttribute('style')
            container.style.display = 'none'
            await Wss.sleep(20)
            aside.style.width = '100%'
        }

        // 向右滑动事件
        touch.right = async function () {
            aside.removeAttribute('style')
            await Wss.sleep(200)
            aside.style.display = 'none'
            container.removeAttribute('style')
        }

        // 初始化拖动事件
        const isTarget = event => event.path.find(e => e === aside || e === container)
        let x, y;
        document.addEventListener('touchstart', event => {
            if (!isTarget(event)) return
            x = event.touches[0].pageX
            y = event.touches[0].pageY
        }, false)
        document.addEventListener('touchend', event => {
            if (!isTarget(event)) return
            let spanX = event.changedTouches[0].pageX - x
            let spanY = event.changedTouches[0].pageY - y
            if (Math.abs(spanX) > Math.abs(spanY)) {
                spanX > offset && touch.left()
                spanX < -offset && touch.right()
            }
        }, false)
        touch.right()
        return touch
    }
}

// 录音机
class AudioRecorder {
    // 定时器
    timer;
    // 超时时间
    timeout;
    // 解码器
    decoder;
    // 运行状态
    run;
    // 录制开始时间
    startTime;

    constructor(timeout) {
        this.timeout = timeout
        this.run = false
    }

    start() {
        return new Promise(async (resolve, reject) => {
            if (this.run)
                await this.stop()
            Wss.getUserMedia(Ws.audio).then(async stream => {
                this.decoder = new Recorder(stream)
                this.decoder.start()
                this.startTime = Date.now()
                this.run = true
                // 超时停止录制
                this.timer = setTimeout(async () => {
                    await this.stop()
                    resolve(this.getBlob())
                }, this.timeout * 1000)
            }).catch(() => {
                reject('媒体授权被拒绝')
            })
        })
    }

    stop() {
        if (!this.run)
            return
        return new Promise(async (resolve, reject) => {
            await this.decoder.stop()
            clearTimeout(this.timer)
            this.run = false
            if (Math.round((Date.now() - this.startTime) / 1000) < 1) {
                reject('说话时间太短')
            } else {
                this.endTime = Date.now()
                resolve()
            }
        })
    }

    getBlob() {
        if (this.run)
            throw 'recorder is running!'
        const blob = this.decoder.getBlob()
        const time = Math.round((this.endTime - this.startTime) / 1000)
        return {
            blob: blob,
            time: time
        }
    }
}

// 视频录像机
class VideoRecorder {
    stream;
    run;
    recorder;

    constructor() {
        this.run = false
    }

    initMediaStream(camera) {
        return new Promise((resolve, reject) => {
            Wss.getUserMedia([Ws.audio, Ws.video], camera).then(stream => {
                this.stream = stream
                this.recorder = new MediaRecorder(stream)
                resolve(stream)
            }).catch(e => {
                reject(e)
            })
        })
    }

    start() {
        return new Promise((resolve, reject) => {
            this.recorder.ondataavailable = e => {
                resolve(e.data)
            }
            this.recorder.onerror = e => {
                reject(e)
            }
            this.recorder.start()
            this.run = true
        })
    }

    stop() {
        this.run && this.recorder.stop();
        this.run = false
    }

    dispose() {
        this.stop();
        this.stream && this.stream.getTracks().forEach(track => track.stop());
    }
}

// 音乐播放器
class MediaPlayer {
    // 运行状态
    run;
    // 播放器对象
    player;
    // 是否重复播放
    loop;

    constructor(loop) {
        this.player = new Audio()
        this.loop = loop
        this.run = false
    }

    /**
     * 开始播放
     * @param uri 音频链接
     */
    start(uri) {
        this.player.src = uri
        return new Promise(async (resolve, reject) => {
            await this.stop()
            this.run = true
            this.player.addEventListener('error', () => reject('error'), false)
            this.player.addEventListener('ended', () => {
                this.loop ? this.player.play() : this.stop()
                resolve()
            }, false)
            this.player.play().catch(e => {
                // 准备播放状态被终止的错误将会忽略
                !e.toString().includes('interrupted') && reject('error')
            })
        })
    }

    // 停止播放
    stop() {
        return new Promise(resolve => {
            Wss.sleep(100).then(async () => {
                await this.player.pause()
                this.player.currentTime = 0
                this.run = false
                resolve()
            })
        })
    }
}

const Clipboarder = {
    /**
     * 解析粘贴板，根据相关内容返回
     * 若粘贴板内容为文本，不会申请权限
     */
    async read(event) {
        // 粘贴板包含文字 直接返回
        const text = (event.clipboardData).getData('text')
        if (text)
            return text
        if (!Support.clipboard) {
            console.warn('您的浏览器不支持读取粘贴板图片操作')
            return ''
        }
        const clipboard = await navigator.clipboard.read()
        const item = clipboard[0], type = item.types[0]
        const blob = await item.getType(type)
        const data = await blob.text()
        if (type === 'text/html')
            return /src="(.+)"/.exec(data)[1]
        if (type.startsWith('image'))
            return blob
        return ''
    }
}

/**
 * WebRTC快速连接工具
 */
class WebRTC {
    local;
    remote;
    client;
    stream;
    connected;
    type;
    // 前后摄像头状态
    camera;

    /**
     * 构建基础RTC框架
     *
     * @param local 本地视频
     * @param remote 远程视频
     */
    constructor(local, remote) {
        this.local = local || new Audio()
        this.remote = remote || new Audio()
        this.type = arguments.length ? Ws.video : Ws.audio
        this.local.muted = this.remote.muted = this.camera = true
        this.connected = false
    }

    /**
     * 添加stream媒体流
     */
    initMediaStream(stream) {
        this.stream = stream
        this.client = new RTCPeerConnection({
            "iceServers": [{
                'url': 'stun:stun.l.google.com:19302'
            }],
            "iceTransportPolicy": "all",
            "blockStatus": "NOTBLOCKED"
        })
        stream.getTracks().forEach(track => this.client.addTrack(track, stream))
    }

    /**
     * 初始化对等连接对象（连接成功前，远程视频流显示本地）
     *
     * @param candidate 候选人回调
     * @param ready 本地连接准备就绪回调
     * @param done 连接成功回调
     */
    initPeerConnection(candidate, ready, done) {
        const client = this.client
        // 先将远程媒体设置成本地流（连接成功后恢复远程流）
        this.remote.srcObject = this.stream
        // 远程流监视
        let remoteStream;
        client.ontrack = event => {
            if (event && event.streams) {
                remoteStream = event.streams[0]
            }
        }
        // 候选人监视
        client.onicecandidate = event => event.candidate && candidate(event.candidate)
        this.local.onloadeddata = () => this.local.play()
        this.remote.onloadeddata = () => this.remote.play()
        // 连接出错事件
        client.onicecandidateerror = event => console.log(event)
        client.addEventListener("iceconnectionstatechange", () => {
            if (client.iceConnectionState === "connected") {
                // 取消远程会话静音
                this.remote.muted = false
                this.connected = true
                this.local.srcObject = this.stream
                this.remote.srcObject = remoteStream
                done()
            }
        })
        ready()
    }

    /**
     * 创建远程链接信令
     * @return Promise 远程信令sdp
     */
    createOffer() {
        const client = this.client
        return new Promise(async (resolve) => {
            const offer = await client.createOffer()
            await client.setLocalDescription(offer)
            resolve(offer.sdp)
        })
    }

    /**
     * 创建远程链接应答
     * @param sdp 远程信令sdp
     * @return Promise 远程应答sdp
     */
    createAnswer(sdp) {
        const client = this.client
        return new Promise(async resolve => {
            const description = new RTCSessionDescription({
                type: Ws.offer,
                sdp: sdp
            })
            await client.setRemoteDescription(description)
            const answer = await client.createAnswer()
            await client.setLocalDescription(answer)
            resolve(answer.sdp)
        })
    }

    /**
     * 设置远程链接应答
     * @param sdp 远程应答sdp
     */
    async setAnswer(sdp) {
        const description = new RTCSessionDescription({
            type: Ws.answer,
            sdp: sdp
        })
        await this.client.setRemoteDescription(description)
    }

    /**
     * 添加候选者信息
     * @param candidate 候选人
     */
    async addCandidate(candidate) {
        await this.client.addIceCandidate(new RTCIceCandidate(candidate))
    }

    // 释放媒体资源
    dispose() {
        this.client && this.client.close()
        this.stream && this.stream.getTracks().forEach(track => track.stop())
        this.local && (this.local.srcObject = null)
        this.remote && (this.remote.srcObject = null)
        this.connected = false
        this.camera = true
        this.client = null
    }

    // 媒体状态切换
    switchTrack(drive) {
        if (this.stream == null)
            return false
        for (const track of this.stream.getTracks()) {
            if (track.kind === drive) {
                track.enabled = !track.enabled
                return track.enabled
            }
        }
    }

    // 本地远程视频画面切换
    switchLocalRemote() {
        const local = this.local, remote = this.remote
        if (local && local.srcObject && this.connected) {
            [local.srcObject, remote.srcObject] = [remote.srcObject, local.srcObject];
            [local.muted, remote.muted] = [remote.muted, local.muted];
        }
    }

    // 前后摄像头切换
    switchCamera() {
        return new Promise((resolve, reject) => {
            // 终止视频流
            this.stream.getVideoTracks().forEach(track => {
                track.stop()
                this.stream.removeTrack(track)
            })
            // 添加新的视频流
            Wss.getUserMedia(Ws.video, this.camera).then(async stream => {
                const track = stream.getTracks()[0]
                this.stream.addTrack(track, stream)
                const sender = this.client.getSenders().find(s => s.track.kind === Ws.video)
                await sender?.replaceTrack(track)
                this.camera = !this.camera
                resolve(this.camera)
            }).catch(e => {
                reject(e)
            })
        })
    }
}

// 兼容性测试工具
const Support = {
    download: 'download' in document.createElement('a'),
    media: !!navigator.mediaDevices,
    backdrop: CSS.supports('backdrop-filter', 'blur(1px)'),
    webrtc: 'RTCPeerConnection' in window,
    observer: !!window.ResizeObserver,
    clipboard: !!navigator?.clipboard?.read,
    notify: !!window.Notification,
}

const Wss = {
    /**
     * 元素移动事件
     *
     * @param target 触发元素
     * @param move 移动元素（默认为触发元素）
     * @param refer 参照元素（不会移出参照元素范围，默认参照body）
     */
    moveBind(target, move, refer) {
        if (!target) return
        refer = refer || document.body
        move = move || target
        // 按下事件
        target.addEventListener(ismobile ? 'touchstart' : 'mousedown', event => {
            event.stopPropagation()
            const ctop = (event.clientY || event.touches?.item(0).clientY) - move.offsetTop
            // 创建移动事件
            const moveEvent = event => {
                event.preventDefault()
                const top = (event.clientY || event.touches?.item(0).clientY) - ctop
                const y = Math.max(0, Math.min(refer.clientHeight - move.clientHeight, top))
                move.style.top = `${y}px`
            }
            document.addEventListener(ismobile ? 'touchmove' : 'mousemove', moveEvent, {
                passive: false
            })
            // 松开时移除移动事件
            document.addEventListener(ismobile ? 'touchend' : 'mouseup', event => {
                event.stopPropagation()
                document.removeEventListener(ismobile ? 'touchmove' : 'mousemove', moveEvent)
            })
        })
    },

    /**
     * 格式化秒
     */
    formatSeconds(value) {
        let second = ~~value
        let min = 0
        let hour = 0
        if (second >= 60) {
            min = ~~(second / 60)
            second = second % 60
            if (min >= 60) {
                hour = ~~(min / 60)
                min = min % 60
            }
        }
        const fill = s => s < 10 ? '0' + s : s
        return (hour ? (fill(hour) + ':') : '') + fill(min) + ':' + fill(second)
    },

    /**
     * 消息时间距离判断
     */
    isExpired(list, timeout, time) {
        timeout = timeout * 1000
        const ft = list => list.first().createTime
        const lt = list => list.last().createTime
        return time ? list.length > 0 && ft(list) - time > timeout : list.length === 0 || Date.now() - lt(list) > timeout
    },

    /**
     * 解析时间
     */
    parseTime(second) {
        if (second < 60)
            return second + "秒"
        if (second < 60 * 60)
            return Math.floor(second / 60) + "分钟"
        const hours = Math.floor(second / (60 * 60))
        if (second < 60 * 60 * 24)
            return hours + "小时"
        const days = Math.floor(second / (60 * 60 * 24))
        return days + "天" + Math.floor(hours - days * 24) + "小时"
    },

    /**
     * 格式化时间 格式：xx月xx日 [xx时:xx分] <br>
     * 非当天时间仅显示[xx月xx日]，若额外显示[xx时:xx分]可指定act参数为true <br>
     * 当天时间仅显示[xx时:xx分] <br>
     *
     * @param timestamp 手动指定时间
     * @param act 显示时分
     */
    formatTime(timestamp, act) {
        const date = new Date(timestamp || Date.now())
        const fill = s => s < 10 ? '0' + s : s
        const dawn = new Date(new Date().toLocaleDateString()).getTime()
        const MMdd = (date.getMonth() + 1) + '月' + date.getDate() + '日'
        const hours = date.getHours()
        const dict = {
            '5,6,7': '早上',
            '8,9,10,11': '上午',
            '12': '中午',
            '13,14,15,16,17': '下午',
            '18,19,20,21,22,23': '晚上'
        }
        const ck = dict[Object.keys(dict).find(e => e.split(',').includes(String(hours)))]
        const hhmm = (ck || '凌晨') + ' ' + fill(hours > 12 ? hours - 12 : hours) + ':' + fill(date.getMinutes())
        return timestamp < dawn ? (act ? MMdd + ' ' + hhmm : MMdd) : hhmm
    },

    /**
     * 获得媒体访问授权
     * @param media[] 媒体参数
     * @param environment 是否获取后置摄像头（如果有）
     */
    async getUserMedia(media, environment) {
        if (!isSecureContext) {
            throw '由于浏览器安全策略，请使用HTTPS访问此网站。'
        }
        if (navigator.mediaDevices) {
            return await navigator.mediaDevices.getUserMedia({
                audio: media.includes(Ws.audio),
                video: media.includes(Ws.video) ? environment ? {
                    facingMode: "environment",
                } : true : false
            })
        }
        throw '您的浏览器不支持此功能'
    },

    /**
     * ES6线程休眠
     */
    sleep(ms) {
        return new Promise(resolve => setTimeout(() => resolve(), ms || 10))
    },

    /**
     * 获得ws连接地址
     */
    getWsURL(URL) {
        const suffix = window.location.protocol.replace('http', '')
        return 'ws' + suffix + '//' + window.location.host + URL
    },

    /**
     * 选择文件快捷入口
     */
    chooseFile(size, type) {
        let accept;
        switch (type) {
            case Ws.audio:
                accept = '.mp3,.aac,.wav'
                break;
            case Ws.video:
                accept = '.mp4,.webm'
                break;
            case Ws.img:
                accept = '.jpg,.jpeg,.png,.bmp'
                break;
        }
        return new Promise((resolve, reject) => {
            const input = document.createElement('input')
            input.type = 'file'
            input.accept = accept
            input.click()
            input.onchange = async () => {
                let file = input.files[0]
                file.size > size ? reject('文件大小超过限制') : resolve(file)
            }
        })
    },

    /**
     * 压缩图片数据
     */
    async zipImage(blob, size) {
        if (blob.size > size * 10) {
            throw '图片大小超过预设过多无法压缩'
        }
        const blobURL = []
        let quality = 1, zoom = 1
        const zip = blob => new Promise((resolve, reject) => {
            const image = new Image()
            blobURL.push(image.src = URL.createObjectURL(blob))
            image.onload = function () {
                const canvas = document.createElement("canvas")
                canvas.width = image.width * zoom
                canvas.height = image.height * zoom
                const ctx = canvas.getContext("2d")
                ctx.scale(zoom, zoom)
                ctx.drawImage(image, 0, 0)
                canvas.toBlob(blob => resolve(blob), "image/jpeg", quality)
            }
            image.onerror = () => reject('无效的图片格式')
        })
        while (blob.size > size) {
            blob = await zip(blob)
            quality -= 0.05
            zoom -= 0.1
        }
        blobURL.forEach(url => URL.revokeObjectURL(url))
        return blob
    },

    /**
     * 获取图片实际尺寸（也可验证图片是否有效）
     */
    loadImage(url) {
        return new Promise((resolve, reject) => {
            if (url) {
                const image = new Image()
                image.src = url
                image.onload = () => resolve({
                    width: image.width,
                    height: image.height
                })
                image.onerror = () => reject('无效的图片资源地址')
            } else {
                resolve()
            }
        })
    },

    /**
     * 获取音频长度
     */
    loadAudio(url) {
        return new Promise((resolve, reject) => {
            const player = new Audio()
            player.src = url
            player.addEventListener('loadeddata', () => {
                resolve({duration: Math.round(player.duration)})
            })
            player.addEventListener('error', () => reject('无效的音频资源地址'))
        })
    },

    /**
     * 获取视频实际大小与第一帧图片数据（也可以验证视频是否有效）
     */
    loadVideo(url) {
        return new Promise((resolve, reject) => {
            const video = document.createElement("video");
            const canvas = document.createElement("canvas");
            const ctx = canvas.getContext("2d")
            video.src = url
            video.muted = true
            video.autoplay = true
            video.preload = 'auto'
            video.addEventListener('loadeddata', () => {
                const width = canvas.width = video.videoWidth;
                const height = canvas.height = video.videoHeight;
                ctx.drawImage(video, 0, 0, width, height);
                canvas.toBlob(blob => resolve({
                    frame: blob,
                    width: width,
                    height: height,
                    duration: Math.round(video.duration)
                }));
                // 清空视频资源
                video.pause()
            });
            video.addEventListener('error', () => reject('无效的视频资源地址'))
            video.play()
        })
    },

    /**
     * 弹出桌面通知
     */
    showNotify(title, content) {
        window?.notify?.close()
        window.notify = new Notification('【' + title + '】', {
            body: '消息：' + content
        })
        Wss.sleep(5000).then(() => notify.close())
    }
}
