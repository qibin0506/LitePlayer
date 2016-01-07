#liteplayer
liteplayer是android上的一款开源音乐播放器<br />

## 注意： 可能会因为百度音乐网页的变更，网络搜索、下载部分变为失效，请自行修改抓取规则。

#特点：<br />
播放界面仿QQ音乐<br />
网络音乐抓取自百度音乐，所以不需要额外服务器<br />
自动抓取音乐歌词，并多形式展示<br />
UI简洁美观，打造最lite的音乐播放器<br />

#播放原理：<br />
liteplayer并没有使用广播控制进度的显示，而是采用回调机制。具体流程：<br />
![image](https://github.com/qibin0506/LitePlayer/blob/master/images/pro.png)<br />
播放服务PlayService提供一系列的播放、切换方法供绑定后的activity调用。<br /><br />


#部分截图：<br />
![image](https://github.com/qibin0506/LitePlayer/blob/master/images/splash.png)<br />
![image](https://github.com/qibin0506/LitePlayer/blob/master/images/1.png)
![image](https://github.com/qibin0506/LitePlayer/blob/master/images/2.png)<br />
![image](https://github.com/qibin0506/LitePlayer/blob/master/images/3.png)
![image](https://github.com/qibin0506/LitePlayer/blob/master/images/4.png)<br />
![image](https://github.com/qibin0506/LitePlayer/blob/master/images/5.png)
![image](https://github.com/qibin0506/LitePlayer/blob/master/images/6.png)<br />
