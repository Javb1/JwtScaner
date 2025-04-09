## Jwt Scaner

用于爆破JWT密钥的burp插件

简单无脑的爆破，首先会将JWT的header和body解码显示在Results中：

如果算法是HS256或其他算法，请自行手工更换

![image-20250409095308540](README/image-20250409095308540.png)

**内置字典370602条，爆破时会将字典中的原字符串形态，base64编码形态，hex编码形态都进行爆破**

Attacklogs中会输出爆破密钥的日志

![image-20250409095511975](README/image-20250409095511975.png)

爆破成功案例：用jwt.io中的案例jwt

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

![image-20250409095655191](README/image-20250409095655191.png)



安装：通过burp Extensions安装即可。