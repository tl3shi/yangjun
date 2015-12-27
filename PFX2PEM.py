
def pfx2pem(inf1, inf2):
    import subprocess
    cmd = 'cmd.exe C:\\OpenSSL-Win64\\PFX2PEM.bat'
    p = subprocess.Popen("cmd.exe /c" + "C:\\OpenSSL-Win64\\PFX2PEM.bat" + " " + inf1 + " " + inf2, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)


    curline = p.stdout.readline()
    while(curline != b''):
        print(curline)
        curline = p.stdout.readline()
    p.wait()
    print(p.returncode)

if __name__ == '__main__':
    pfx2pem("C:\\OpenSSL-Win64\\pl_2013.pfx", "C:\\OpenSSL-Win64\\pl_2013.chain")

