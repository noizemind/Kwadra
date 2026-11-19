const { describe, it } = require("node:test");
const assert = require("node:assert/strict");

const DTMF = {
  "1": [697, 1209],
  "2": [697, 1336],
  "0": [941, 1336],
  "#": [941, 1477],
};

describe("clearline dtmf map", () => {
  it("has classic dual-tone pairs", () => {
    assert.deepEqual(DTMF["1"], [697, 1209]);
    assert.equal(DTMF["0"][0], 941);
    assert.ok(Object.keys(DTMF).length >= 4);
  });
});
