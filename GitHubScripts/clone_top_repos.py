#!/usr/bin/python

import os
import sys


def project_url_generator( projects_file, limit ):
	projects_yielded = 0
	with open( projects_file, 'r' ) as projects:
		for line in projects:
			project_url = line.split(',')[0]
			if project_url == 'repository_url': continue
			projects_yielded += 1
			yield project_url
			if projects_yielded >= limit: break


def get_project_repo_name( project_url ):
	user, project_name = project_url.split('/')[-2:]
	return '%s.%s' % (user, project_name)


def get_project_repo_name_slash( project_url ):
	user, project_name = project_url.split('/')[-2:]
	return '%s/%s' % (user, project_name)


def main():
	if len(sys.argv) != 6:
		print 'Error: Invalid number of arguments.'
		print 'Usage: projects_list_csv_file folder_to_store_dataset num_projects_to_download git_username git_password'
		sys.exit(1)
	
	projects_list_file = sys.argv[1]
	dataset_folder = sys.argv[2]
	projects_num_to_download = int(sys.argv[3])
	git_username = sys.argv[4]
	git_passwd = sys.argv[5]
	for project_url in project_url_generator( projects_list_file, projects_num_to_download ):
		print 'Downloading project:', get_project_repo_name( project_url )
		project_dir = '%s/%s' % (dataset_folder, get_project_repo_name( project_url ))
		os.system('git clone https://%s:%s@github.com/%s %s' % (git_username, git_passwd, get_project_repo_name_slash(project_url), project_dir))


if __name__ == "__main__":
	main()

