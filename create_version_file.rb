#!/usr/bin/ruby1.9.1

require 'open3'

def run(cmd, wd=Dir.pwd)
  puts wd+" $ "+cmd
  Open3.popen3(cmd, :chdir=>wd) do |i,o,e,t|
    puts o.read.chomp
    puts e.read.chomp
  end
end

run "git fetch --tags"
run "git tag > VERSIONS"

versions = []

IO.readlines('VERSIONS').each do |v|
  major, minor, patch = v.match(/\Av([0-9]+)\.([0-9]+)\.([0-9]+)\Z/).captures
  versions << [major.to_i, minor.to_i, patch.to_i]
end
File.delete("VERSIONS")

versions.sort! do |a,b|
  if a[0]!=b[0]
    a[0]-b[0]
  elsif a[1]!=b[1]
    a[1]-b[1]
  else
    a[2]-b[2]
  end
end

v = versions[-1]
version = "v#{v[0]}.#{v[1]}.#{v[2]}"

run "echo #{version} > VERSION"
