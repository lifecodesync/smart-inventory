# Bugs log (cause → fix → test that prevents it)

| # | Symptom | Root cause | Fix | Prevented by |
|---|---|---|---|---|
| 1 | Best/slow-sellers query would fail with a SQL syntax error | Java text blocks strip trailing spaces, so `"""…ORDER BY """ + order` became `ORDER BYunits` | Concatenate an explicit `" "` | `DashboardAndForecastTest.summaryAndSalesMatchHandCalculation` |
| 2 | (Design-time) Two staff selling the last item could make stock −1 | "Read stock, check, then save" is two steps; both threads read 1 | Single atomic `UPDATE … WHERE stock >= qty` + DB `CHECK` | `ConcurrentSaleTest` (10 threads × 5 runs) |
| 3 | (Design-time) Sales at 11:30 PM IST counted on the wrong day | Server runs in UTC; 23:30 IST = 18:00 UTC same day, but 05:00 IST = 23:30 UTC previous day | Day boundaries computed with `AT TIME ZONE 'Asia/Kolkata'` | Verified in SQL; `DashboardAndForecastTest` |

Add every real bug you hit while running and deploying it here. Interviewers ask for exactly this.
