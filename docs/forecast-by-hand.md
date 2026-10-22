# Forecast worked by hand (matches ForecasterTest)

Samosa sales for 7 days: **20, 22, 18, 25, 30, 10, 2** (the last two are Saturday and Sunday).

**Moving average (window 7):** (20+22+18+25+30+10+2) / 7 = 127 / 7 = **18.14 per day**, which gives a 7-day demand of 127.

**Exponential smoothing (α = 0.5):** start level = 20
- 0.5·22 + 0.5·20 = 21
- 0.5·18 + 0.5·21 = 19.5
- 0.5·25 + 0.5·19.5 = 22.25
- 0.5·30 + 0.5·22.25 = 26.125
- 0.5·10 + 0.5·26.125 = 18.06
- 0.5·2 + 0.5·18.06 = **10.03 per day**

Smoothing reacts fast to the weekend dip. The moving average is steadier.

**MAE (which method was right more often):** for each of the last N days, forecast it using only earlier days, then take |actual − forecast| and average them. Example with [10, 20, 30, 40] and a 2-day MA: the forecasts are 15 and 25, the actuals are 30 and 40, so the errors are 15 and 15 and **MAE = 15**.

**Safety stock:** σ (std-dev of daily sales) = 4, z = 1.65, lead time = 4 days → 1.65 × 4 × √4 = 13.2 → **14**.

**Restock:** predicted 70 + safety 10 − stock 30 = **50**. If stock is 500, the result is max(0, negative) = **0**.
