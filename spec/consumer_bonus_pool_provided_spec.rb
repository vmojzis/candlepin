# -*- coding: utf-8 -*-
require 'spec_helper'
require 'candlepin_scenarios'
require 'rexml/document'

describe 'Consumer Resource Host/Guest' do

  include CandlepinMethods

  before(:each) do
    @owner1 = create_owner random_string('test_owner1')
    @username1 = random_string("user1")
    @user1 = user_client(@owner1, @username1)
  end

  it 'bonus pool should have provided products' do
    uuid1 = random_string('system.uuid')
    guests = [{'guestId' => uuid1}]

    provided_product = create_product(random_string('product'),
      random_string('product'),
      {:owner => @owner1['key']})

    std_product = create_product(random_string('product'),
      random_string('product'),
      {:attributes => {:virt_limit => "5",
        :host_limited => 'true'},
        :owner => @owner1['key'], :providedProducts => [provided_product['id']]})

    # Make sure to create the pool with a source subscription and upstream pool id,
    # in order for the bonus pool to be created
    @cp.create_pool(@owner1['key'], std_product.id, { :quantity => 10,
      :subscription_id => random_str('source_sub'),
      :upstream_pool_id => random_str('upstream')})

    all_pools =  @user1.list_owner_pools(@owner1['key'])
    all_pools.size.should == 2
    unmapped_pool = all_pools.find {|p| p['type'] == 'UNMAPPED_GUEST'} 
    normal_pool = all_pools.find {|p| p['type'] == 'NORMAL'} 
    
    normal_pool['providedProducts'].size.should == 1
    unmapped_pool['providedProducts'].size.should == 1
  end

end

