from pathlib import Path
import json

OUT = Path(__file__).parent

def patterns(n):
    if n == 0:
        yield ()
        return
    a = [0] * n
    def grow(k, greatest):
        if k == n:
            yield tuple(a)
        else:
            for value in range(greatest + 2):
                a[k] = value
                yield from grow(k + 1, max(value, greatest))
    yield from grow(1, 0)

def shape(a, memo):
    n=len(a); b=a[::-1]
    if not memo:
        # Arithmetic DAG for uncached execution counts, not a cached algorithm.
        c=[[1]*(n+1) for _ in range(n+1)]
        branches=[[0]*(n+1) for _ in range(n+1)]
        depths=[[1]*(n+1) for _ in range(n+1)]
        for i in range(1,n+1):
            for j in range(1,n+1):
                deps=[(i-1,j-1)] if a[i-1]==b[j-1] else [(i-1,j),(i,j-1)]
                c[i][j]=1+sum(c[x][y] for x,y in deps)
                branches[i][j]=1+sum(branches[x][y] for x,y in deps)
                depths[i][j]=1+max(depths[x][y] for x,y in deps)
        calls=c[n][n]; branch=branches[n][n]
        return dict(calls=calls,branchEvents=branch,events=3*calls+branch+2,maxStack=depths[n][n])
    cache={}; counts=dict(calls=0,hits=0,branchEvents=0,maxStack=0)
    def go(i,j,depth):
        counts['calls']+=1;counts['maxStack']=max(counts['maxStack'],depth)
        if (i,j) in cache:
            counts['hits']+=1
            return cache[i,j]
        if i==0 or j==0: value=0
        else:
            counts['branchEvents']+=1
            if a[i-1]==b[j-1]:value=1+go(i-1,j-1,depth+1)
            else:value=max(go(i-1,j,depth+1),go(i,j-1,depth+1))
        cache[i,j]=value
        return value
    answer=go(n,n,1)
    return dict(**counts,events=3*counts['calls']+counts['branchEvents']+2,evaluations=len(cache),answer=answer)

def witness(a):return ''.join(chr(97+x) for x in a)

def palindrome_oracle(a):
    return max(len(s) for mask in range(1 << len(a))
               if (s := tuple(a[i] for i in range(len(a)) if mask & (1 << i))) == s[::-1])

def maxima(n):
    count=0; result={'n':n,'recursion':{},'memoization':{}}
    for a in patterns(n):
        count+=1
        for mode,memo in [('recursion',False),('memoization',True)]:
            values=shape(a,memo)
            if memo:
                assert values['answer'] == palindrome_oracle(a), (a, values)
            for metric,value in values.items():
                if metric=='answer':continue
                prev=result[mode].get(metric)
                if prev is None or value>prev['value']:
                    result[mode][metric]={'value':value,'witness':witness(a),'shape':values,'ties':1}
                elif value==prev['value']:prev['ties']+=1
    result['equalityPatterns']=count
    return result

rows=[maxima(n) for n in range(1,9)]
assert [r['equalityPatterns'] for r in rows] == [1,2,5,15,52,203,877,4140]
(OUT/'arithmetic.json').write_text(json.dumps({'basis':'Exhaustive restricted-growth equality patterns; pure count arithmetic, no production execution, API or JVM','childOrder':'drop text prefix before drop reversed prefix; equal characters use diagonal only','events':'3*calls + uncached nonbase evaluations + 2 start/done','rows':rows},indent=2)+'\n')
for r in rows:
    print(r['n'],r['equalityPatterns'],'rec',r['recursion']['calls']['value'],r['recursion']['events']['value'],r['recursion']['calls']['witness'],'memo',r['memoization']['calls']['value'],r['memoization']['events']['value'],r['memoization']['calls']['witness'])

examples={s:{'recursion':shape(tuple(s),False),'memoization':shape(tuple(s),True)} for s in ['bbbab','cbbd','abcdef','abcdefg','abcdefgh','abbbbb','bbbbab','abcabc','abcba']}
(OUT/'examples.json').write_text(json.dumps(examples,indent=2)+'\n')

# Maximal per-event estimates; all possible vars are charged simultaneously,
# even though several event kinds never have all of them. Integer values here
# upper-bound every accepted n<=8 state's displayed lengths/counts.
variables={'event':'cache-hit','state':'i=8,j=8','calls':'129','cacheHits':'129',
           'stateEvaluations':'81','computedStates':'81','callId':'129',
           'value':'8','answer':'8'}
var_bytes=sum(len(k)+len(v)+8 for k,v in variables.items())
labels=sum(len('i='+str(i))+4 for i in range(9))+sum(len('j='+str(i))+4 for i in range(9))
table_bytes=36+labels+2*9+34*81+16
stack_bytes=16*(len('lps(i=8,j=8) #129')+4)
per_step=240+512+var_bytes+table_bytes+stack_bytes
(OUT/'memo-budget.json').write_text(json.dumps({
    'basis':'Conservative existing StepEmitter estimator arithmetic, not actual runtime/wire measurement',
    'assumptions':['lps or lcs function; i=/j= prefix labels; single 9x9 table',
        'helper vars only; no slice/domain vars; no other structures',
        'narration <=256 printable ASCII chars; charge2 bytes each for escaping',
        'all cells unknown max34; every frame maximally long on every event',
        'base envelope240; null DP formula/substitution overcharged16'],
    'variablesUpper':variables,'variableBytes':var_bytes,'tableBytes':table_bytes,
    'stackBytes':stack_bytes,'narrationBytes':512,'baseBytes':240,
    'perStepUpperEstimate':per_step,'genericEventsUpper':453,
    'genericTotalUpperEstimate':per_step*453,
    'reverseSpecificEventsUpper':383,'reverseSpecificTotalUpperEstimate':per_step*383,
    'under2000000':per_step*453 < 2000000},indent=2)+'\n')
