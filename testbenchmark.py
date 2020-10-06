
import os
import collections
import itertools

ground_dir = 'code/test/'
eval_dir = 'mypointeranalysis/'

def loadfile(fname):
    f = open(fname)
    # result = {}
    result = collections.defaultdict(set)
    for line in f:
        line = line.strip()
        if not line:
            continue
        x, y = line.split(':')
        x = int(x.strip())
        y = y.strip()
        if not y:
            y = set()
        else:
            y = y.split(' ')
            y = set((int(i) for i in y))
        if x in result:
            print('dup')
        # result[x] = y
        result[x].update(y)
    return result

totalscore = 0

for fin in os.listdir(ground_dir):
    if fin.endswith('.stdout.txt'):
        f = fin[:-11]
    elif fin.endswith('.stdout'):
        f = fin[:-7]
    else:
        continue
    ground_file = loadfile(os.path.join(ground_dir, fin))
    myoutput_file = loadfile(os.path.join(eval_dir, 'result.'+f+'.txt'))

    issound = True
    for i, ground in ground_file.items():
        eval = myoutput_file[i]
        for g in ground:
            if g not in eval:
                issound = False
                print(f, i, ground, eval)
    
    if issound:
        ground_total = len(list(itertools.chain(*ground_file.values())))
        my_total = len(list(itertools.chain(*myoutput_file.values())))
        score = (ground_total+1)/(my_total+1) + 1
    else:
        score = 0
    totalscore += score
    print(f, issound, score)

print('score:', totalscore)
