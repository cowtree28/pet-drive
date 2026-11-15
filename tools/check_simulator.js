// 시뮬레이터(prototype/index.html)의 점수 함수가 Java MotionRiskScorer와 같은 값을 내는지 확인한다.
// 사용: node tools/check_simulator.js
const fs = require('fs');
const html = fs.readFileSync(__dirname + '/../prototype/index.html', 'utf8');
const src = html.match(/const clamp01[\s\S]*?\nfunction decide/)[0].replace(/\nfunction decide$/, '');
const { score, setReports } = new Function(`${src.replace('let reports = 0;', 'let reports = 0;')}; return { score, setReports: n => { reports = n; } };`)();
const cases = [ // [멀미 있음, 급가감속, 휴식 후 분, 온도, 음성 횟수, 기대 점수, 기대 단계]
  [true, 1, 42, 23, 0, 22, '편안'], [true, 5, 67, 24.3, 0, 58, '주의'], [true, 7, 74, 25.9, 0, 81, '위험'],
  [true, 1, 42, 23, 1, 37, '편안'], [true, 50, 500, 40, 2, 100, '위험'], [true, 0, 0, 10, 0, 0, '편안'],
];
let fail = 0;
for (const [sens, h, m, t, v, want, lvl] of cases) {
  setReports(v);
  const r = score(sens, h, m, t);
  const ok = r.total === want && r.level === lvl;
  if (!ok) fail++;
  console.log(`${ok ? 'PASS' : 'FAIL'} 급가감속 ${h}, ${m}분, ${t}℃, 음성 ${v}회 → ${r.total}점 ${r.level} (기대 ${want} ${lvl})`);
}
console.log(fail ? `${fail} FAILED` : 'ALL PASS');
process.exit(fail ? 1 : 0);
