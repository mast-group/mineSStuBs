#!/usr/bin/python3

import json
import random
import sys

SAMPLES = 300

with open( sys.argv[1], 'r') as f:
    sstubs = json.load(f)

commit_IDs = set()
for sstub in sstubs:                                 
    commit_IDs.add(sstub['commitSHA1'])

sample_indices = random.sample(range(len(commit_IDs)), SAMPLES)
ids_list = list(commit_IDs)
sampled_commit_IDs = [ids_list[index] for index in sample_indices]

# Create the urls for the sampled commits
for id in sampled_commit_IDs:
    for sstub in sstubs:
        if sstub['commitSHA1'] == id:
            url = 'https://github.com/%s/%s/commit/%s/' % (sstub['projectName'].split('.')[0], sstub['projectName'][sstub['projectName'].find('.') + 1: ], id)
            print(url)
            break

