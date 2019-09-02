package com.example.administrator.mildweather;


import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.mildweather.db.City;
import com.example.administrator.mildweather.db.Country;
import com.example.administrator.mildweather.db.Province;
import com.example.administrator.mildweather.util.HttpUtil;
import com.example.administrator.mildweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


/**
 * Created by ${windy} on 27.
 * ChooseAreaFragment
 */

public class ChooseAreaFragment extends Fragment{
    //定义常量
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTRY=2;
    private ProgressDialog progressDialog;
    private TextView title_text;
    private Button back_button;
    private ListView list_view;
    private ArrayAdapter<String> adapter;
    private List<String> dataList=new ArrayList<>();
    //列表
    private List<Province> provinceList;
    private List<City> cityList;
    private List<Country> countryList;
    private int currentLevel;//当前选中的级别
    private Province selectedProvince;
    private City selectedCity;
    private static final String TAG = "ChooseAreaFragment";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //初始化控件
        View view =inflater.inflate(R.layout.choose_area,container,false);
        title_text=(TextView)view.findViewById(R.id.title_text);
        back_button=(Button)view.findViewById(R.id.back_button);
        list_view=(ListView)view.findViewById(R.id.list_view);
        adapter=new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1,dataList);//item待修改
        list_view.setAdapter(adapter);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //监听事件
        list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel==LEVEL_PROVINCE){
                    selectedProvince=provinceList.get(position);
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
                    selectedCity=cityList.get(position);
                    queryCounties();
                }

            }
        });

        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel==LEVEL_COUNTRY){
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
                    queryProvinces();
                }
            }
        });

        queryProvinces();//加载省数据

    }

    /**
     * 查询所有Province，优先数据库
     */
    private void queryProvinces() {
        title_text.setText("中国");
        back_button.setVisibility(View.GONE);
        provinceList= DataSupport.findAll(Province.class);//查询数据库
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
//                Log.d(TAG, "queryProvinces:"+province.getId()+"---"+province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            list_view.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else{
            //查询服务器
            String address="http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }

    }

    /**
     * 查询所有Cities，优先数据库
     */
    private void queryCities() {
        title_text.setText(selectedProvince.getProvinceName());
        back_button.setVisibility(View.VISIBLE);
        cityList=DataSupport.where("provinceid = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size()>0){
            dataList.clear();
            for(City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            list_view.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            String address="http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");

        }

    }

    /**
     * 查询所有Counties，优先数据库
     */
    private void queryCounties() {
        title_text.setText(selectedCity.getCityName());
        back_button.setVisibility(View.VISIBLE);
        countryList=DataSupport.where("cityid = ?",String.valueOf(selectedCity.getId())).find(Country.class);
        if(countryList.size()>0){
            countryList.clear();
            for(Country country:countryList){
                dataList.add(country.getCountryName());
            }
            adapter.notifyDataSetChanged();
            list_view.setSelection(0);
            currentLevel=LEVEL_COUNTRY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            int cityCode=selectedCity.getCityCode();
            String address="http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"country");
        }

    }

    private void queryFromServer(String address,final String type) {
        showProgressDialog();
        HttpUtil.sendOkhttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDiaglog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();
                boolean result=false;
                if("province".equals(type)){
                    result= Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result=Utility.handleCitiesResponse(responseText,selectedProvince.getId());
                }else if("country".equals(type)){
                    result=Utility.handleCountriesResponse(responseText,selectedCity.getId());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDiaglog();
                            if("province".equals(type)){
                                queryProvinces();//重新查询，本地数据库加载，更新页面
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("country".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });

    }

    private void showProgressDialog() {
        if(progressDialog==null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDiaglog() {
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }

}
