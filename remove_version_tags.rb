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

version = ARGV[0]
puts "removing version tags for version #{version}"

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

  puts dir
  run "git tag -d #{version}","../#{dir}"
  run "git push origin :refs/tags/#{version}","../#{dir}"

end
