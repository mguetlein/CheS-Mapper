#!/usr/bin/ruby1.9.1

require 'open3'

def run(cmd, wd=Dir.pwd)
  puts wd+" $ "+cmd
  res = nil
  Open3.popen3(cmd, :chdir=>wd) do |i,o,e,t|
    res = o.read.chomp
    puts res
    puts e.read.chomp
  end
  res
end

if ARGV.size!=1
  $stderr.puts "version missing" 
  exit 1
end

["CheS-View", "CheS-Map", "JavaLib"].each do |f|
  raise "#{f} is apparently a symlink, change back to submodule directories" if File.symlink?(f)
end

version = ARGV[0]
puts "releasing current dev version as #{version}"
File.delete("VERSION") if File.exist?("VERSION")

major, minor, patch = version.match(/\Av([0-9]+)\.([0-9]+)\.([0-9]+)\Z/).captures

class String
  def is_int?
    self.to_i>0 or self=="0"
  end
end

unless major.is_int? and minor.is_int? and patch.is_int?
  $stderr.puts "version illegal" 
  exit 1
end

["CheS-View", "CheS-Map", "JavaLib", "ches-mapper"].each do |dir|
#dir = "JavaLib"

  puts dir
  res = run("git branch")
  raise "not on dev" unless res=~/\*\sdev/
  puts " merge dev into master"
  run "git checkout master","../#{dir}"
  res = run("git merge dev","../#{dir}")
  if dir=="ches-mapper"
      ["CheS-View", "CheS-Map", "JavaLib"].each do |sub|
        puts " update submodule"
        run "git fetch --tags","../ches-mapper/#{sub}"
        run "git checkout #{version}","../ches-mapper/#{sub}"
        run "git add #{sub}", "../ches-mapper"
      end
  end
  if dir!="ches-mapper" and res=~/Already up-to-date./
    puts "nothing to commit for #{dir}"
  else
    run "git commit -m \"merging dev for new release #{version}\"","../#{dir}"
    run "git push origin master","../#{dir}"
  end
  puts " set version tag"
  run "git tag -a #{version} -m \"version #{version}\"","../#{dir}"
  run "git push origin #{version}","../#{dir}"
  run "git checkout dev","../#{dir}"
end

new_v = "v#{major}.#{minor.to_i+1}.#{0}"
puts "set new dev version #{new_v}"
File.open("VERSION", 'w') {|f| f.write(new_v) }

