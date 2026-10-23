// Explicit evidence profiles only; this is not production algorithm routing.
const family=process.env.DP_AUDIT_FAMILY||'linear-grid';
if(!['linear-grid','circular-activity-obstacles','falling-triangle','subset-family','capacity-family'].includes(family))throw Error('Unknown DP evidence profile');
module.exports={family,...require(`./${family}-cases.cjs`)};
