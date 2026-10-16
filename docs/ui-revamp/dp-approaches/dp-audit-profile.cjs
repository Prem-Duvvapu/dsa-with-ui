// Explicit evidence profiles only; this is not production algorithm routing.
const family=process.env.DP_AUDIT_FAMILY||'linear-grid';
if(!['linear-grid','circular-activity-obstacles'].includes(family))throw Error('Unknown DP evidence profile');
module.exports={family,...require(`./${family}-cases.cjs`)};
