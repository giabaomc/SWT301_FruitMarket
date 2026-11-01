$chrome = "C:\Program Files\Google\Chrome\Application\chrome.exe"
$outDir = "C:\Users\ganda\OneDrive\Documents\SWT301"
$prjDir = "c:\Users\ganda\OneDrive\Documents\PRJ\Fruit_Market\test\evidence"
if (!(Test-Path $prjDir)) { New-Item -ItemType Directory -Path $prjDir -Force }

# 1. PS1: Full product list
Write-Host "Capturing PS1..."
$url1 = "http://localhost:9996/Fruit_Market/productjsp?service=listProduct"
Start-Process -FilePath $chrome -ArgumentList "--headless", "--disable-gpu", "--screenshot=$outDir\PS1_R1.png", "--window-size=1280,900", $url1 -Wait
Copy-Item "$outDir\PS1_R1.png" "$prjDir\PS1_R1.png" -Force

# 2. PS2: Search 'Tao' with 'Xoa loc' button
Write-Host "Capturing PS2..."
$url2 = "http://localhost:9996/Fruit_Market/productjsp?service=listProduct&productName=Tao&submit=T%C3%ACm"
Start-Process -FilePath $chrome -ArgumentList "--headless", "--disable-gpu", "--screenshot=$outDir\PS2_R1.png", "--window-size=1280,900", $url2 -Wait
Copy-Item "$outDir\PS2_R1.png" "$prjDir\PS2_R1.png" -Force

# 3. PS3: Update product 10 to status=0 in DB, then show search defect
Write-Host "Setting status=0 for product 10..."
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -pgiabao2006 -e "USE Assignment; UPDATE tblProducts SET status = 0 WHERE productID = 10;"
$url3 = "http://localhost:9996/Fruit_Market/productjsp?service=listProduct&productName=Thom+mat&submit=T%C3%ACm"
Start-Process -FilePath $chrome -ArgumentList "--headless", "--disable-gpu", "--screenshot=$outDir\PS3_R1.png", "--window-size=1280,900", $url3 -Wait
Copy-Item "$outDir\PS3_R1.png" "$prjDir\PS3_R1.png" -Force

# 4. PS4: Search with whitespace ' Tao Fuji '
Write-Host "Capturing PS4..."
$url4 = "http://localhost:9996/Fruit_Market/productjsp?service=listProduct&productName=+Tao+Fuji+&submit=T%C3%ACm"
Start-Process -FilePath $chrome -ArgumentList "--headless", "--disable-gpu", "--screenshot=$outDir\PS4_R1.png", "--window-size=1280,900", $url4 -Wait
Copy-Item "$outDir\PS4_R1.png" "$prjDir\PS4_R1.png" -Force

# 5. PS5: Add to cart from search and open cart
Write-Host "Capturing PS5..."
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest -Uri "http://localhost:9996/Fruit_Market/cart?service=add2cart&productID=1" -WebSession $session -UseBasicParsing | Out-Null
$cartUrl = "http://localhost:9996/Fruit_Market/cart?service=showCart"
$cartHtml = (Invoke-WebRequest -Uri $cartUrl -WebSession $session -UseBasicParsing).Content
$cartTemp = "c:\Users\ganda\OneDrive\Documents\PRJ\Fruit_Market\web\temp_cart_ps5.html"
[System.IO.File]::WriteAllText($cartTemp, $cartHtml, [System.Text.Encoding]::UTF8)
$url5 = "http://localhost:9996/Fruit_Market/temp_cart_ps5.html"
Start-Process -FilePath $chrome -ArgumentList "--headless", "--disable-gpu", "--screenshot=$outDir\PS5_R1.png", "--window-size=1280,900", $url5 -Wait
Copy-Item "$outDir\PS5_R1.png" "$prjDir\PS5_R1.png" -Force
Remove-Item $cartTemp -ErrorAction SilentlyContinue

# 6. PS6: XSS Injection attempt
Write-Host "Capturing PS6..."
$url6 = "http://localhost:9996/Fruit_Market/productjsp?service=listProduct&productName=%3Cscript%3Ealert(1)%3C%2Fscript%3E&submit=T%C3%ACm"
Start-Process -FilePath $chrome -ArgumentList "--headless", "--disable-gpu", "--screenshot=$outDir\PS6_R1.png", "--window-size=1280,900", $url6 -Wait
Copy-Item "$outDir\PS6_R1.png" "$prjDir\PS6_R1.png" -Force

# Reset product 10 status back to 1
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -pgiabao2006 -e "USE Assignment; UPDATE tblProducts SET status = 1 WHERE productID = 10;"

Write-Host "All screenshots captured successfully!"
Get-ChildItem "$outDir\PS*_R1.png" | Select-Object Name, Length
