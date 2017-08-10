#!/bin/bash

main() {
  hadolint
  dockerlint
  dockerfile_lint
}

hadolint() {
  echo -e "\nrunning hadolint..."
  docker run --rm -i lukasmartinelli/hadolint < Dockerfile
  if [ -n $? ]; then
    echo -e "Hadolint found errors, continuing.\n"
  else
    echo -e "Hadolint passed without errors\n"
  fi
}

dockerlint() {
  echo "\nrunning dockerlint..." 
  docker run -i --rm -v "$PWD/Dockerfile":/Dockerfile:ro \
  redcoolbeans/dockerlint
  if [ -n $? ]; then
    echo -e "Dockerlint found errors, continuing.\n"
  else
    echo -e "Dockerlint passed without errors\n"
  fi
}

dockerfile_lint() {
  echo -e "\nrunning dockerfile_lint..."
  docker run -i --rm -v `pwd`:/root/ projectatomic/dockerfile-lint \
    dockerfile_lint -f Dockerfile
  if [ -n $? ]; then
    echo -e "Dockerlint found errors, continuing.\n"
  else
    echo -e "Dockerlint passed without errors\n"
  fi
}

main "$@"
